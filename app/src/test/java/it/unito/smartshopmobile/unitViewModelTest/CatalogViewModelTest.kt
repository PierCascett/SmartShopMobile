/**
 * CatalogViewModelTest.kt
 *
 * Suite di test unitari per il CatalogViewModel.
 * Copre flussi customer: filtri catalogo, carrello, submit ordini
 * e sincronizzazione storico ordini, usando MockK + Robolectric.
 */
package it.unito.smartshopmobile.unitViewModelTest

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import it.unito.smartshopmobile.data.dao.CategoryDao
import it.unito.smartshopmobile.data.dao.OrderDao
import it.unito.smartshopmobile.data.dao.ProductDao
import it.unito.smartshopmobile.data.dao.ShelfDao
import it.unito.smartshopmobile.data.database.SmartShopDatabase
import it.unito.smartshopmobile.data.datastore.FavoritesDataStore
import it.unito.smartshopmobile.data.datastore.SessionDataStore
import it.unito.smartshopmobile.data.entity.Category
import it.unito.smartshopmobile.data.entity.OrderWithLines
import it.unito.smartshopmobile.data.entity.Product
import it.unito.smartshopmobile.data.entity.User
import it.unito.smartshopmobile.data.remote.RetrofitInstance
import it.unito.smartshopmobile.data.repository.CategoryRepository
import it.unito.smartshopmobile.data.repository.OrderRepository
import it.unito.smartshopmobile.data.repository.ProductRepository
import it.unito.smartshopmobile.data.repository.ShelfRepository
import it.unito.smartshopmobile.data.repository.UserRepository
import it.unito.smartshopmobile.viewModel.AvailabilityFilter
import it.unito.smartshopmobile.viewModel.CatalogViewModel
import it.unito.smartshopmobile.viewModel.DeliveryMethod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifica il comportamento del CatalogViewModel sui casi principali lato cliente:
 * - toggle filtri (offerte, disponibilita, tab)
 * - aggiornamento contatti cliente e stato utente loggato
 * - gestione carrello (add/remove, validazione stock, messaggi toast)
 * - submit ordini (validazioni su indirizzo, carrello vuoto, successo)
 * - refresh storico ordini e simulazione ritiro locker
 *
 * Usa flussi fake per repository/DAO e il dispatcher di test per controllare le coroutine.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class CatalogViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val productFlow = MutableStateFlow<List<Product>>(emptyList())
    private val categoryFlow = MutableStateFlow<List<Category>>(emptyList())
    private val shelfFlow = MutableStateFlow<List<it.unito.smartshopmobile.data.entity.Shelf>>(emptyList())
    private val ordersFlow = MutableStateFlow<List<OrderWithLines>>(emptyList())
    private val favoritesFlow = MutableStateFlow<Set<String>>(emptySet())
    private val sessionFlow = MutableStateFlow<User?>(null)

    /**
     * Helper per gestire setup.
     */
    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Finta API Retrofit e singleton
        mockkObject(RetrofitInstance)
        every { RetrofitInstance.api } returns mockk(relaxed = true)

        // Fake database + DAO rilassati
        mockkObject(SmartShopDatabase.Companion)
        val fakeDb = mockk<SmartShopDatabase>(relaxed = true)
        every { fakeDb.productDao() } returns mockk<ProductDao>(relaxed = true)
        every { fakeDb.categoryDao() } returns mockk<CategoryDao>(relaxed = true)
        every { fakeDb.orderDao() } returns mockk<OrderDao>(relaxed = true)
        every { fakeDb.shelfDao() } returns mockk<ShelfDao>(relaxed = true)
        every { SmartShopDatabase.getDatabase(any()) } returns fakeDb

        // Costruttori dei repository e DataStore mockati
        mockkConstructor(ProductRepository::class, CategoryRepository::class, ShelfRepository::class, OrderRepository::class, UserRepository::class, FavoritesDataStore::class, SessionDataStore::class)

        every { anyConstructed<ProductRepository>().getAllProducts() } returns productFlow
        coEvery { anyConstructed<ProductRepository>().refreshProducts() } returns Result.success(Unit)

        every { anyConstructed<CategoryRepository>().getAllCategories() } returns categoryFlow
        coEvery { anyConstructed<CategoryRepository>().refreshCategories() } returns Result.success(Unit)

        every { anyConstructed<ShelfRepository>().getAll() } returns shelfFlow
        coEvery { anyConstructed<ShelfRepository>().refresh() } returns Result.success(Unit)

        every { anyConstructed<OrderRepository>().observeOrders() } returns ordersFlow
        coEvery { anyConstructed<OrderRepository>().refreshOrders() } returns Result.success(Unit)
        coEvery { anyConstructed<OrderRepository>().updateOrderStatus(any(), any()) } returns Result.success(Unit)
        coEvery { anyConstructed<OrderRepository>().createOrder(any()) } returns Result.success(
            it.unito.smartshopmobile.data.entity.OrderCreated(1, 10.0)
        )

        coEvery { anyConstructed<UserRepository>().updateProfile(any(), any()) } returns Result.failure(Exception("not used"))
        coEvery { anyConstructed<UserRepository>().uploadAvatar(any(), any(), any()) } returns Result.failure(Exception("not used"))

        every { anyConstructed<SessionDataStore>().userFlow } returns sessionFlow
        coEvery { anyConstructed<SessionDataStore>().saveUser(any()) } just runs
        coEvery { anyConstructed<SessionDataStore>().clear() } just runs

        every { anyConstructed<FavoritesDataStore>().favoritesFlow(any()) } returns favoritesFlow
        coEvery { anyConstructed<FavoritesDataStore>().saveFavorites(any(), any()) } just runs
    }

    /**
     * Pulisce tutti i mock dopo ogni test.
     */
    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Test: onOnlyOffersToggle commuta il flag offerte.
     */
    @Test
    fun onOnlyOffersToggle_switchesFlag() {
        // Arrange: stato iniziale con onlyOffers false
        val vm = CatalogViewModel(context)

        // Act: premiamo il toggle
        vm.onOnlyOffersToggle()

        // Assert: il flag è stato ribaltato a true
        assertTrue(vm.uiState.value.onlyOffers)
    }

    /**
     * Helper per gestire set customer contacts updates address and phone.
     */
    @Test
    fun setCustomerContacts_updatesAddressAndPhone() {
        // Arrange: ViewModel pronto
        val vm = CatalogViewModel(context)

        // Act: impostiamo indirizzo e telefono per la consegna
        vm.setCustomerContacts("Via Torino 10", "0123456789")

        // Assert: i campi nello stato riflettono i valori inseriti
        val state = vm.uiState.value
        assertEquals("Via Torino 10", state.shippingAddress)
        assertEquals("0123456789", state.shippingPhone)
    }

    /**
     * Helper per gestire set logged user stores user and keeps delivery default.
     */
    @Test
    fun setLoggedUser_storesUserAndKeepsDeliveryDefault() {
        // Arrange: utente customer fittizio
        val vm = CatalogViewModel(context)
        val user = User(7, "Carla", "Neri", "carla@example.com", ruolo = "cliente")

        // Act: salviamo l'utente nel ViewModel
        vm.setLoggedUser(user)

        // Assert: utente nello stato e metodo di consegna resta quello di default (locker)
        val state = vm.uiState.value
        assertEquals(user, state.loggedUser)
        assertEquals(DeliveryMethod.LOCKER, state.deliveryMethod)
    }

    /**
     * Helper per gestire on add to cart stops at available stock and shows toast.
     */
    @Test
    fun onAddToCart_stopsAtAvailableStockAndShowsToast() = runTest {
        // Arrange: prodotto con solo 1 pezzo a scaffale
        val product = Product(
            catalogId = 1,
            id = "prd-01",
            name = "Mela",
            brand = "BrandX",
            categoryId = "cat-1",
            categoryName = "Frutta",
            categoryDescription = null,
            catalogQuantity = 1,
            warehouseQuantity = 0,
            totalQuantity = 1,
            price = 2.5,
            oldPrice = null,
            availability = "OK",
            tags = emptyList(),
            description = null,
            imageUrl = null,
            shelfId = 1
        )
        productFlow.value = listOf(product)

        val vm = CatalogViewModel(context)
        advanceUntilIdle()

        // Act: aggiungiamo due volte lo stesso prodotto
        vm.onAddToCart(product.id)
        vm.onAddToCart(product.id)

        // Assert: quantità bloccata a 1 e toast mostrato
        val state = vm.uiState.value
        assertEquals(1, state.cart[product.id])
        assertTrue(state.showToast)
        assertEquals("Disponibili solo 1 pezzi di Mela", state.toastMessage)
    }

    /**
     * Helper per gestire submit order with empty cart returns error.
     */
    @Test
    fun submitOrder_withEmptyCart_returnsError() = runTest {
        // Arrange: utente loggato ma carrello vuoto
        val vm = CatalogViewModel(context)
        vm.setLoggedUser(User(1, "Mario", "Rossi", "mario@example.com", ruolo = "cliente"))
        advanceUntilIdle()

        // Act
        vm.submitOrder()
        advanceUntilIdle()

        // Assert
        val state = vm.uiState.value
        assertEquals("Il carrello è vuoto", state.orderError)
        assertFalse(state.isSubmittingOrder)
    }

    /**
     * Helper per gestire submit order domicilio without address shows validation error.
     */
    @Test
    fun submitOrder_domicilioWithoutAddress_showsValidationError() = runTest {
        // Arrange: utente loggato, carrello pieno ma nessun indirizzo
        val product = Product(
            catalogId = 2,
            id = "prd-02",
            name = "Arancia",
            brand = "BrandY",
            categoryId = "cat-1",
            categoryName = "Frutta",
            categoryDescription = null,
            catalogQuantity = 3,
            warehouseQuantity = 0,
            totalQuantity = 3,
            price = 3.1,
            oldPrice = null,
            availability = "OK",
            tags = emptyList(),
            description = null,
            imageUrl = null,
            shelfId = 1
        )
        productFlow.value = listOf(product)

        val vm = CatalogViewModel(context)
        vm.setLoggedUser(User(2, "Laura", "Bianchi", "laura@example.com", ruolo = "cliente"))
        advanceUntilIdle()

        vm.onDeliveryMethodSelected(DeliveryMethod.DOMICILIO)
        vm.onAddToCart(product.id)

        // Act
        vm.submitOrder()
        advanceUntilIdle()

        // Assert: validazione indirizzo mancante
        val state = vm.uiState.value
        assertEquals("Aggiungi un indirizzo di spedizione nelle impostazioni", state.orderError)
        assertFalse(state.isSubmittingOrder)
        assertEquals(DeliveryMethod.DOMICILIO, state.deliveryMethod)
        assertEquals(AvailabilityFilter.ALL, state.availabilityFilter)
    }

    /**
     * Test: submitOrder con successo svuota il carrello e imposta lastOrderId.
     */
    @Test
    fun submitOrder_success_clearsCartAndSetsLastOrderId() = runTest {
        // Arrange: utente loggato, carrello con un prodotto e mock createOrder
        val product = Product(
            catalogId = 3,
            id = "prd-03",
            name = "Insalata",
            brand = "BrandZ",
            categoryId = "cat-2",
            categoryName = "Verdure",
            categoryDescription = null,
            catalogQuantity = 5,
            warehouseQuantity = 0,
            totalQuantity = 5,
            price = 1.99,
            oldPrice = null,
            availability = "OK",
            tags = emptyList(),
            description = null,
            imageUrl = null,
            shelfId = 1
        )
        productFlow.value = listOf(product)
        coEvery { anyConstructed<OrderRepository>().createOrder(any()) } returns Result.success(
            it.unito.smartshopmobile.data.entity.OrderCreated(idOrdine = 77, totale = 3.98)
        )

        val vm = CatalogViewModel(context)
        vm.setLoggedUser(User(5, "Luisa", "Bianchi", "luisa@example.com", ruolo = "cliente"))
        advanceUntilIdle()
        vm.onAddToCart(product.id)
        vm.onAddToCart(product.id)

        // Act
        vm.submitOrder()
        advanceUntilIdle()

        // Assert: carrello svuotato, lastOrderId settato e nessun errore
        val state = vm.uiState.value
        assertEquals(77, state.lastOrderId)
        assertTrue(state.cart.isEmpty())
        assertFalse(state.isSubmittingOrder)
        assertNull(state.orderError)
    }

    /**
     * Helper per gestire refresh order history without user sets error message.
     */
    @Test
    fun refreshOrderHistory_withoutUser_setsErrorMessage() = runTest {
        val vm = CatalogViewModel(context)

        vm.refreshOrderHistory()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Accedi per visualizzare lo storico ordini", state.orderHistoryError)
        assertFalse(state.isOrderHistoryLoading)
    }

    /**
     * Helper per gestire simulate locker pickup marks order completed after delay.
     */
    @Test
    fun simulateLockerPickup_marksOrderCompletedAfterDelay() = runTest {
        // Arrange: ordine SPEDITO al locker per utente loggato
        val order = it.unito.smartshopmobile.data.entity.Order(
            idOrdine = 300,
            idUtente = 9,
            dataOrdine = "2025-12-01T12:00:00Z",
            stato = "SPEDITO",
            totale = 10.0,
            metodoConsegna = "LOCKER",
            idLocker = 1,
            codiceRitiro = "XYZ",
            indirizzoSpedizione = null,
            nomeCliente = "Giovanni",
            cognomeCliente = "Verdi",
            emailCliente = "gio@example.com"
        )
        ordersFlow.value = listOf(OrderWithLines(order, emptyList()))

        val vm = CatalogViewModel(context)
        vm.setLoggedUser(User(9, "Giovanni", "Verdi", "gio@example.com", ruolo = "cliente"))
        advanceUntilIdle()

        // Act
        vm.simulateLockerPickup(order.idOrdine)
        // avanza oltre i 15 secondi simulati
        advanceTimeBy(16_000)
        advanceUntilIdle()

        // Assert: updateOrderStatus chiamato e pickup ripulito
        coVerify { anyConstructed<OrderRepository>().updateOrderStatus(order.idOrdine, "CONCLUSO") }
        val state = vm.uiState.value
        assertNull(state.pickupInProgressId)
        assertNull(state.pickupMessage)
    }
}
