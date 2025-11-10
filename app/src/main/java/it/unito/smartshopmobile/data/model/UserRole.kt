/**
 * UserRole.kt
 *
 * RUOLO MVVM: Model Layer (Domain Model)
 * - Definisce i ruoli utente dell'applicazione
 * - Utilizzato per determinare le funzionalità accessibili
 * - Usato da LoginViewModel per routing post-login
 *
 * RESPONSABILITÀ:
 * - Enum con tutti i ruoli possibili (CUSTOMER, EMPLOYEE, MANAGER)
 * - Ogni ruolo ha title e description per la UI
 *
 * PATTERN: Value Object (immutabile)
 * - Rappresenta un concetto di business (ruolo utente)
 * - Usato per navigazione e controllo accessi
 */
package it.unito.smartshopmobile.data.model

enum class UserRole(val title: String, val description: String) {
    CUSTOMER(
        title = "Cliente",
        description = "Esplora il catalogo, riempi il carrello, prenota locker o consegna."
    ),
    EMPLOYEE(
        title = "Dipendente",
        description = "Gestisci gli ordini in tempo reale e assegna locker o consegne."
    ),
    MANAGER(
        title = "Responsabile",
        description = "Controlla magazzino e scaffali, invia richieste ai fornitori."
    )
}
