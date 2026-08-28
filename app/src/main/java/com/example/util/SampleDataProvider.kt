package com.example.util

import com.example.data.local.Voter

object SampleDataProvider {
    fun getSampleVoters(): List<Voter> {
        return listOf(
            Voter(
                cedula = "4.582.109",
                fullName = "GONZÁLEZ ACOSTA, JUAN CARLOS",
                votingPlace = "Colegio Nacional Capital",
                tableNumber = "12",
                orderNumber = "045",
                address = "Av. Mcal. López 1450, Barrio Villa Morra",
                cityOrZone = "Asunción",
                phone = "+595 981 123 456",
                notes = "Coordinador de mesa de votación.",
                voted = true
            ),
            Voter(
                cedula = "3.921.847",
                fullName = "BENÍTEZ ROJAS, MARIA ELENA",
                votingPlace = "Escuela Básica Nº 12 Rep. de Colombia",
                tableNumber = "05",
                orderNumber = "112",
                address = "Calle 14 de Mayo 890",
                cityOrZone = "Centro",
                phone = "+595 971 234 567",
                notes = "Requiere asistencia para voto accesible.",
                voted = false
            ),
            Voter(
                cedula = "5.102.394",
                fullName = "MARTÍNEZ SILVA, CARLOS ALBERTO",
                votingPlace = "Colegio Nacional Capital",
                tableNumber = "12",
                orderNumber = "198",
                address = "Av. España c/ Brasilia",
                cityOrZone = "Asunción",
                phone = "+595 983 345 678",
                notes = "Contactado telefónicamente.",
                voted = true
            ),
            Voter(
                cedula = "2.845.612",
                fullName = "RODRÍGUEZ ORTIZ, ANA BEATRIZ",
                votingPlace = "Escuela Graduada Nº 45",
                tableNumber = "01",
                orderNumber = "015",
                address = "Calle Palma 432",
                cityOrZone = "Asunción",
                phone = "+595 982 456 789",
                notes = "Miembro activo del comité local.",
                voted = false
            ),
            Voter(
                cedula = "4.120.985",
                fullName = "LOPEZ CARDOZO, FRANCISCO JAVIER",
                votingPlace = "Colegio Nacional Comercio Nº 1",
                tableNumber = "08",
                orderNumber = "077",
                address = "Av. Fernando de la Mora 2100",
                cityOrZone = "Lambaré",
                phone = "+595 991 567 890",
                notes = "Confirmó que asistirá temprano.",
                voted = true
            ),
            Voter(
                cedula = "5.890.123",
                fullName = "GIMÉNEZ TORRES, PATRICIA ISABEL",
                votingPlace = "Escuela Básica Nº 12 Rep. de Colombia",
                tableNumber = "05",
                orderNumber = "210",
                address = "Calle Oliva 650",
                cityOrZone = "Centro",
                phone = "+595 985 678 901",
                notes = "",
                voted = false
            ),
            Voter(
                cedula = "3.450.781",
                fullName = "DÍAZ MENDOZA, ROBERTO DANIEL",
                votingPlace = "Colegio Técnico Vocacional",
                tableNumber = "03",
                orderNumber = "143",
                address = "Av. Eusebio Ayala km 4.5",
                cityOrZone = "San Lorenzo",
                phone = "+595 984 789 012",
                notes = "Sugerir transporte gratuito.",
                voted = true
            ),
            Voter(
                cedula = "4.981.234",
                fullName = "VERA VILLALBA, GLADYS LILIANA",
                votingPlace = "Colegio Nacional Capital",
                tableNumber = "14",
                orderNumber = "089",
                address = "Av. Sacramento 1120",
                cityOrZone = "Trinidad",
                phone = "+595 972 890 123",
                notes = "",
                voted = false
            ),
            Voter(
                cedula = "6.123.456",
                fullName = "AMARILLA PERALTA, DIEGO ARMANDO",
                votingPlace = "Escuela Graduada Nº 45",
                tableNumber = "02",
                orderNumber = "034",
                address = "Calle Alberdi 1050",
                cityOrZone = "Asunción",
                phone = "+595 981 901 234",
                notes = "Joven primer votante registrado.",
                voted = true
            ),
            Voter(
                cedula = "3.789.012",
                fullName = "FERREIRA CABRERA, LAURA CONCEPCIÓN",
                votingPlace = "Colegio Nacional Comercio Nº 1",
                tableNumber = "08",
                orderNumber = "156",
                address = "Av. Mcal. Estigarribia 880",
                cityOrZone = "Fernando de la Mora",
                phone = "+595 992 012 345",
                notes = "",
                voted = false
            ),
            Voter(
                cedula = "5.321.654",
                fullName = "AQUINO DUARTE, GUSTAVO ADOLFO",
                votingPlace = "Colegio Técnico Vocacional",
                tableNumber = "03",
                orderNumber = "201",
                address = "Calle Mcal. Estigarribia 304",
                cityOrZone = "San Lorenzo",
                phone = "+595 983 123 456",
                notes = "Confirmado por vía WhatsApp.",
                voted = true
            ),
            Voter(
                cedula = "4.234.567",
                fullName = "SANTACRUZ RUIZ, SOFÍA ELIZABETH",
                votingPlace = "Escuela Básica Nº 12 Rep. de Colombia",
                tableNumber = "06",
                orderNumber = "012",
                address = "Calle Cerro Corá 1290",
                cityOrZone = "Centro",
                phone = "+595 982 234 567",
                notes = "",
                voted = false
            ),
            Voter(
                cedula = "2.943.810",
                fullName = "ACUÑA BENÍTEZ, RAMÓN IGNACIO",
                votingPlace = "Colegio Nacional Capital",
                tableNumber = "12",
                orderNumber = "019",
                address = "Av. Mcal. López c/ San Martín",
                cityOrZone = "Asunción",
                phone = "+595 981 765 432",
                notes = "Confirmado para votar a primera hora.",
                voted = false
            ),
            Voter(
                cedula = "3.109.876",
                fullName = "NUÑEZ PEÑA, CARMEN BEATRIZ",
                votingPlace = "Escuela Básica Nº 12 Rep. de Colombia",
                tableNumber = "05",
                orderNumber = "088",
                address = "Calle Palma 1140",
                cityOrZone = "Centro",
                phone = "+595 971 890 321",
                notes = "",
                voted = true
            )
        )
    }
}
