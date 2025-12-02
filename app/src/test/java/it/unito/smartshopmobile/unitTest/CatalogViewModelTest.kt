package it.unito.smartshopmobile

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.coEvery
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
import it.unito.smartshopmobile.viewModel.CatalogViewModel
import it.unito.smartshopmobile.viewModel.DeliveryMethod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun onOnlyOffersToggle_switchesFlag() {
        // Arrange: stato iniziale con onlyOffers false
        val vm = CatalogViewModel(context)

        // Act: premiamo il toggle
        vm.onOnlyOffersToggle()

        // Assert: il flag è stato ribaltato a true
        assertTrue(vm.uiState.value.onlyOffers)
    }

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
}
