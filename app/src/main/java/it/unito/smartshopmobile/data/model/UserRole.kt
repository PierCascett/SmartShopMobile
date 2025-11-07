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
