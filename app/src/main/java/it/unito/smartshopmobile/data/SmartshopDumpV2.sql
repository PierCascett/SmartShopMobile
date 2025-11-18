--
-- PostgreSQL database dump
--

\restrict pDSyM2lVOcuYr4oYYEpOemQ2rryZxixI0FKv4hjcx7QnKg0K95c33k9QW82wZ1p

-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

-- Started on 2025-11-17 18:16:26

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 5136 (class 0 OID 24703)
-- Dependencies: 235
-- Data for Name: catalogo; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.catalogo (id_catalogo, id_prodotto, quantita_disponibile, prezzo, vecchio_prezzo, id_scaffale) FROM stdin;
1	prd-01	2	2.49	2.99	1
4	prd-01	1	2.49	2.99	2
3	prd-03	8	1.99	2.20	3
2	prd-02	18	3.10	3.49	1
5	prd-04	20	1.99	2.29	4
6	prd-05	30	3.49	3.99	5
7	prd-06	25	2.79	3.19	6
8	prd-07	25	2.49	2.89	6
9	prd-08	15	5.99	6.49	7
10	prd-09	18	4.99	5.49	7
11	prd-10	12	4.79	5.29	7
12	prd-11	20	4.49	4.99	8
13	prd-12	10	7.99	8.49	9
14	prd-13	8	9.99	10.49	9
15	prd-14	18	1.59	1.89	3
16	prd-15	16	1.79	2.09	3
17	prd-16	14	1.99	2.29	2
18	prd-17	24	1.49	1.79	10
19	prd-18	30	1.39	1.69	10
20	prd-19	22	1.29	1.59	10
\.


--
-- TOC entry 5123 (class 0 OID 24593)
-- Dependencies: 222
-- Data for Name: categorie_prodotti; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.categorie_prodotti (id_categoria, nome, descrizione, id_sovracategoria) FROM stdin;
1	Detersivi	Prodotti per pulizia e manutenzione	1
2	Lampadine	Illuminazione domestica	1
3	Igiene Orale	Prodotti per la cura dei denti	2
4	Shampoo e Bagnoschiuma	Prodotti per capelli e corpo	2
5	Manzo	Tagli di carne bovina	3
6	Pollo	Carni bianche di pollo	3
7	Tritato	Carne tritata pronta all’uso	3
8	Congelato	Pesce surgelato	4
9	Fresco	Pesce fresco da banco	4
10	Crudo	Pesce per consumo crudo	4
11	Zucchine	Verdure fresche selezionate	5
12	Insalata	Vari tipi di insalate	5
13	Melanzana	Varietà di melanzane fresche	5
14	Stagione	Frutta di stagione	6
15	Esotica	Frutta esotica importata	6
16	Energetiche	Bevande energetiche	7
17	Zero	Bevande senza zucchero	7
18	Tè	Tè freddo e infusi	7
\.


--
-- TOC entry 5127 (class 0 OID 24613)
-- Dependencies: 226
-- Data for Name: fornitori; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.fornitori (id_fornitore, nome, telefono, email, indirizzo) FROM stdin;
1	OrtoVerde Srl	+390612345001	ordini@ortoverde.it	Via dei Campi 12, Roma
2	AgrumiSud Spa	+390892345002	vendite@agrumisud.it	Contrada Aranci 5, Reggio Calabria
3	Fresco&Co	+390212345003	info@frescoco.it	Via Mercato 9, Milano
\.


--
-- TOC entry 5134 (class 0 OID 24683)
-- Dependencies: 233
-- Data for Name: magazzino; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.magazzino (id_magazzino, id_prodotto, quantita_disponibile, id_ultimo_riordino_arrivato) FROM stdin;
2	prd-02	38	\N
1	prd-01	80	1
3	prd-03	38	3
\.


--
-- TOC entry 5130 (class 0 OID 24639)
-- Dependencies: 229
-- Data for Name: ordini; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ordini (id_ordine, id_utente, data_ordine, stato, totale) FROM stdin;
3	3	2025-11-14 18:10:00	PAGATO	32.40
1	1	2025-11-13 10:30:00	CREATO	31.10
2	2	2025-11-14 15:45:00	CONSEGNATO	5.97
4	4	2025-11-17 11:52:42.91752	CREATO	2.49
5	4	2025-11-17 12:01:59.904219	CREATO	7.58
6	4	2025-11-17 12:02:19.427122	CREATO	1.99
7	5	2025-11-17 12:20:43.633427	CREATO	5.09
8	5	2025-11-17 13:10:08.016807	CREATO	1.99
9	4	2025-11-17 13:43:32.190967	CREATO	2.49
10	4	2025-11-17 14:42:33.832698	CREATO	2.49
11	4	2025-11-17 14:56:36.267859	CREATO	3.98
12	4	2025-11-17 15:10:25.877123	CREATO	4.98
13	4	2025-11-17 15:12:31.825531	CREATO	4.98
14	5	2025-11-17 15:18:17.896057	CREATO	2.49
15	5	2025-11-17 15:18:25.293239	CREATO	2.49
16	5	2025-11-17 17:42:58.703412	CREATO	1.99
\.


--
-- TOC entry 5128 (class 0 OID 24623)
-- Dependencies: 227
-- Data for Name: prodotti; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.prodotti (id_prodotto, nome, marca, id_categoria, descrizione) FROM stdin;
prd-01	Mele Fuji	OrtoVerde	14	Mele Fuji croccanti, confezione da 1kg
prd-02	Arance Tarocco	AgrumiSud	14	Arance Tarocco ricche di Vitamina C
prd-03	Insalata mista	Fresco&Co	12	Insalata mista pronta da condire, busta 250g
prd-04	Detersivo Piatti Limone	CasaPulita	1	Detersivo liquido per piatti al limone, flacone 1L
prd-05	Lampadina LED 10W	LumiLux	2	Lampadina LED 10W attacco E27, luce calda
prd-06	Dentifricio Sbiancante	SmilePlus	3	Dentifricio sbiancante uso quotidiano
prd-07	Shampoo Delicato 250ml	Care&Clean	4	Shampoo delicato per uso frequente
prd-08	Fettine di Manzo 400g	CarniPrime	5	Fettine di manzo selezionato 400g
prd-09	Petto di Pollo 500g	CarniPrime	6	Petto di pollo fresco 500g
prd-10	Carne Tritata 400g	CarniPrime	7	Carne tritata mista 400g
prd-11	Filetti di Merluzzo Surgelati 400g	MareNord	8	Filetti di merluzzo surgelati 400g
prd-12	Orata Fresca 500g	MareNord	9	Orata fresca circa 500g
prd-13	Salmone per Sushi 200g	MareNord	10	Trancio di salmone per sushi 200g
prd-14	Zucchine chiare 500g	OrtoVerde	11	Zucchine chiare fresche 500g
prd-15	Melanzane lunghe 1kg	OrtoVerde	13	Melanzane lunghe fresche 1kg
prd-16	Mango maturo 1 pezzo	Tropico	15	Mango maturo pronto da consumare
prd-17	Energy Drink 250ml	PowerMax	16	Bevanda energetica lattina 250ml
prd-18	Cola Zero 1.5L	BubbleSoft	17	Bibita cola zero zuccheri 1.5L
prd-19	Tè freddo limone 1.5L	FreshTea	18	Tè freddo al limone 1.5L
\.


--
-- TOC entry 5137 (class 0 OID 24718)
-- Dependencies: 236
-- Data for Name: prodotti_tag; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.prodotti_tag (id_prodotto, id_tag) FROM stdin;
prd-01	1
prd-01	2
prd-02	3
\.


--
-- TOC entry 5139 (class 0 OID 24736)
-- Dependencies: 238
-- Data for Name: righe_ordine; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.righe_ordine (id_riga, id_ordine, id_prodotto, quantita, prezzo_unitario, prezzo_totale) FROM stdin;
1	1	prd-01	10	2.49	24.90
2	1	prd-02	2	3.10	6.20
3	2	prd-03	3	1.99	5.97
4	4	prd-01	1	2.49	2.49
5	5	prd-01	1	2.49	2.49
6	5	prd-02	1	3.10	3.10
7	5	prd-03	1	1.99	1.99
8	6	prd-03	1	1.99	1.99
9	7	prd-02	1	3.10	3.10
10	7	prd-03	1	1.99	1.99
11	8	prd-03	1	1.99	1.99
12	9	prd-01	1	2.49	2.49
13	10	prd-01	1	2.49	2.49
14	11	prd-03	2	1.99	3.98
15	12	prd-01	2	2.49	4.98
16	13	prd-01	2	2.49	4.98
17	14	prd-01	1	2.49	2.49
18	15	prd-01	1	2.49	2.49
19	16	prd-03	1	1.99	1.99
\.


--
-- TOC entry 5132 (class 0 OID 24658)
-- Dependencies: 231
-- Data for Name: riordini_magazzino; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.riordini_magazzino (id_riordino, id_prodotto, id_fornitore, quantita_ordinata, data_ordine, data_arrivo_prevista, data_arrivo_effettiva, arrivato, id_responsabile) FROM stdin;
1	prd-01	1	100	2025-11-10 09:00:00	2025-11-12	2025-11-12	t	2
2	prd-02	2	80	2025-11-11 10:15:00	2025-11-13	\N	f	2
3	prd-03	3	50	2025-11-09 14:30:00	2025-11-11	2025-11-11	t	2
4	prd-01	1	2	2025-11-16 23:30:22.205855	2026-01-01	\N	f	\N
5	prd-02	2	2	2025-11-17 01:13:50.891229	\N	\N	f	\N
6	prd-02	1	2	2025-11-17 11:15:31.808906	\N	\N	f	\N
\.


--
-- TOC entry 5141 (class 0 OID 32769)
-- Dependencies: 240
-- Data for Name: scaffali; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.scaffali (id_scaffale, nome, descrizione) FROM stdin;
1	Scaffale Frutta 1	Zona frutta vicino ingresso
2	Scaffale Frutta 2	Zona frutta centrale
3	Scaffale Verdura 1	Verdura e insalate
4	Scaffale 4	Poligono mappa #4
5	Scaffale 5	Poligono mappa #5
6	Scaffale 6	Poligono mappa #6
7	Scaffale 7	Poligono mappa #7
8	Scaffale 8	Poligono mappa #8
9	Scaffale 9	Poligono mappa #9
10	Scaffale 10	Poligono mappa #10
11	Scaffale 11	Poligono mappa #11
12	Scaffale 12	Poligono mappa #12
13	Scaffale 13	Poligono mappa #13
14	Scaffale 14	Poligono mappa #14
15	Scaffale 15	Poligono mappa #15
16	Scaffale 16	Poligono mappa #16
17	Scaffale 17	Poligono mappa #17
18	Scaffale 18	Poligono mappa #18
\.


--
-- TOC entry 5143 (class 0 OID 32786)
-- Dependencies: 242
-- Data for Name: sovracategorie; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sovracategorie (id_sovracategoria, nome) FROM stdin;
1	Casa
2	Cura Personale
3	Carne
4	Pesce
5	Verdura
6	Frutta
7	Bevande
\.


--
-- TOC entry 5125 (class 0 OID 24604)
-- Dependencies: 224
-- Data for Name: tag; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.tag (id_tag, nome) FROM stdin;
1	Bio
2	Dolci
3	Vitaminico
4	Produzione Locale
5	0 Zuccheri
6	Proteico
7	Sottovuoto
8	Vegan
9	Eco Friendly
\.


--
-- TOC entry 5121 (class 0 OID 24578)
-- Dependencies: 220
-- Data for Name: utenti; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.utenti (id_utente, nome, cognome, email, telefono, data_creazione, ruolo, password) FROM stdin;
4	casc	casc	casc	\N	2025-11-16 18:16:44.962946	Cliente	$2b$10$Y91rLZQ4wsyWuVdBLCQqfuuvruJ1huGIDERtr96GnDP7qtdwUgmp6
5	pier	pier	pier	\N	2025-11-16 21:16:02.060789	Cliente	$2b$10$CwzxRZ5r3HF9UCVGAk0LCuAr7OKURA23nYxsk6TOaIJrPuK8j65Oi
1	Mario	Rossi	mario	+390612345678	2025-11-15 13:39:21.504754	Cliente	$2b$10$Y91rLZQ4wsyWuVdBLCQqfuuvruJ1huGIDERtr96GnDP7qtdwUgmp6
2	Laura	Bianchi	laura	+390212345679	2025-11-15 13:39:21.504754	Responsabile	$2b$10$Y91rLZQ4wsyWuVdBLCQqfuuvruJ1huGIDERtr96GnDP7qtdwUgmp6
3	Giulia	Verdi	giulia	+390112345680	2025-11-15 13:39:21.504754	Dipendente	$2b$10$Y91rLZQ4wsyWuVdBLCQqfuuvruJ1huGIDERtr96GnDP7qtdwUgmp6
\.


--
-- TOC entry 5160 (class 0 OID 0)
-- Dependencies: 234
-- Name: catalogo_id_catalogo_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.catalogo_id_catalogo_seq', 20, true);


--
-- TOC entry 5161 (class 0 OID 0)
-- Dependencies: 221
-- Name: categorie_prodotti_id_categoria_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.categorie_prodotti_id_categoria_seq', 21, true);


--
-- TOC entry 5162 (class 0 OID 0)
-- Dependencies: 225
-- Name: fornitori_id_fornitore_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.fornitori_id_fornitore_seq', 3, true);


--
-- TOC entry 5163 (class 0 OID 0)
-- Dependencies: 232
-- Name: magazzino_id_magazzino_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.magazzino_id_magazzino_seq', 3, true);


--
-- TOC entry 5164 (class 0 OID 0)
-- Dependencies: 228
-- Name: ordini_id_ordine_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.ordini_id_ordine_seq', 16, true);


--
-- TOC entry 5165 (class 0 OID 0)
-- Dependencies: 237
-- Name: righe_ordine_id_riga_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.righe_ordine_id_riga_seq', 19, true);


--
-- TOC entry 5166 (class 0 OID 0)
-- Dependencies: 230
-- Name: riordini_magazzino_id_riordino_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.riordini_magazzino_id_riordino_seq', 6, true);


--
-- TOC entry 5167 (class 0 OID 0)
-- Dependencies: 239
-- Name: scaffali_id_scaffale_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.scaffali_id_scaffale_seq', 3, true);


--
-- TOC entry 5168 (class 0 OID 0)
-- Dependencies: 241
-- Name: sovracategorie_id_sovracategoria_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sovracategorie_id_sovracategoria_seq', 7, true);


--
-- TOC entry 5169 (class 0 OID 0)
-- Dependencies: 223
-- Name: tag_id_tag_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.tag_id_tag_seq', 9, true);


--
-- TOC entry 5170 (class 0 OID 0)
-- Dependencies: 219
-- Name: utenti_id_utente_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.utenti_id_utente_seq', 5, true);


-- Completed on 2025-11-17 18:16:26

--
-- PostgreSQL database dump complete
--

\unrestrict pDSyM2lVOcuYr4oYYEpOemQ2rryZxixI0FKv4hjcx7QnKg0K95c33k9QW82wZ1p

