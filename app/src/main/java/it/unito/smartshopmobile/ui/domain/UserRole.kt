/**
 * UserRole.kt
 *
 * MVVM: Domain Layer - Value Object per ruoli utente
 *
 * FUNZIONAMENTO:
 * - Enum type-safe per ruoli applicazione (Customer, Employee, Manager)
 * - Helper fromDbRole() per conversione String database → enum
 * - Self-documenting con title e description per UI
 * - Usato per routing navigazione e controllo accessi
 *
 * PATTERN MVVM:
 * - Domain Layer: concetto business separato da Entity database e UI
 * - Value Object: immutabile, equality by value
 * - Type-safety: errori compile-time invece di runtime
 * - Clean Architecture: separazione domain logic da persistenza
 *
 * ESEMPIO:
 * - Database: User.ruolo = "responsabile" (String)
 * - Domain: UserRole.MANAGER (enum type-safe)
 * - UI: UserRole.MANAGER.title = "Responsabile"
 */
package it.unito.smartshopmobile.domain

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
    );

    companion object {
        fun fromDbRole(role: String?): UserRole? = when (role?.lowercase()) {
            "cliente" -> CUSTOMER
            "responsabile" -> MANAGER
            "dipendente" -> EMPLOYEE
            else -> null
        }
    }
}
