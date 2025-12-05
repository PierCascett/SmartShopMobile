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

/**
 * Enumerazione type-safe per i ruoli utente nell'applicazione SmartShop.
 *
 * Rappresenta i tre tipi di utenti supportati dal sistema, ciascuno con:
 * - Titolo visualizzato nell'interfaccia
 * - Descrizione delle responsabilità
 * - Permessi e funzionalità specifiche
 *
 * @property title Nome visualizzato del ruolo nell'UI
 * @property description Descrizione testuale delle responsabilità del ruolo
 */
enum class UserRole(val title: String, val description: String) {
    /**
     * Ruolo cliente: può esplorare il catalogo, gestire il carrello e effettuare ordini.
     * Supporta sia ritiro in locker che consegna a domicilio.
     */
    CUSTOMER(
        title = "Cliente",
        description = "Esplora il catalogo, riempi il carrello, prenota locker o consegna."
    ),

    /**
     * Ruolo dipendente: gestisce il picking degli ordini e le consegne.
     * Ha accesso alla mappa del negozio e alla gestione degli ordini attivi.
     */
    EMPLOYEE(
        title = "Dipendente",
        description = "Gestisci gli ordini in tempo reale e assegna locker o consegne."
    ),

    /**
     * Ruolo manager: supervisiona l'inventario e gestisce i riordini.
     * Può spostare merci tra magazzino e scaffali e contattare i fornitori.
     */
    MANAGER(
        title = "Responsabile",
        description = "Controlla magazzino e scaffali, invia richieste ai fornitori."
    );

    companion object {
        /**
         * Converte una stringa dal database in un valore UserRole type-safe.
         *
         * Mappatura case-insensitive:
         * - "cliente" → CUSTOMER
         * - "dipendente" → EMPLOYEE
         * - "responsabile" → MANAGER
         *
         * @param role Stringa ruolo dal database (nullable)
         * @return UserRole corrispondente, o null se la stringa non è riconosciuta
         */
        fun fromDbRole(role: String?): UserRole? = when (role?.lowercase()) {
            "cliente" -> CUSTOMER
            "responsabile" -> MANAGER
            "dipendente" -> EMPLOYEE
            else -> null
        }
    }
}
