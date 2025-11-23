--
-- PostgreSQL database dump
--


-- Dumped from database version 18.1
-- Dumped by pg_dump version 18.1

-- Started on 2025-11-23 16:01:08

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

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 235 (class 1259 OID 24703)
-- Name: catalogo; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.catalogo (
    id_catalogo integer NOT NULL,
    id_prodotto character varying(20) NOT NULL,
    quantita_disponibile integer NOT NULL,
    prezzo numeric(10,2) NOT NULL,
    vecchio_prezzo numeric(10,2),
    id_scaffale integer NOT NULL
);


ALTER TABLE public.catalogo OWNER TO postgres;

--
-- TOC entry 234 (class 1259 OID 24702)
-- Name: catalogo_id_catalogo_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.catalogo_id_catalogo_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.catalogo_id_catalogo_seq OWNER TO postgres;

--
-- TOC entry 5174 (class 0 OID 0)
-- Dependencies: 234
-- Name: catalogo_id_catalogo_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.catalogo_id_catalogo_seq OWNED BY public.catalogo.id_catalogo;


--
-- TOC entry 222 (class 1259 OID 24593)
-- Name: categorie_prodotti; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.categorie_prodotti (
    id_categoria integer NOT NULL,
    nome character varying(100) NOT NULL,
    descrizione text,
    id_sovracategoria integer
);


ALTER TABLE public.categorie_prodotti OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 24592)
-- Name: categorie_prodotti_id_categoria_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.categorie_prodotti_id_categoria_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.categorie_prodotti_id_categoria_seq OWNER TO postgres;

--
-- TOC entry 5175 (class 0 OID 0)
-- Dependencies: 221
-- Name: categorie_prodotti_id_categoria_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.categorie_prodotti_id_categoria_seq OWNED BY public.categorie_prodotti.id_categoria;


--
-- TOC entry 245 (class 1259 OID 32837)
-- Name: consegna_domicilio; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.consegna_domicilio (
    id_ordine integer NOT NULL,
    id_rider integer,
    data_assegnazione timestamp without time zone DEFAULT now(),
    data_consegna timestamp without time zone
);


ALTER TABLE public.consegna_domicilio OWNER TO postgres;

--
-- TOC entry 226 (class 1259 OID 24613)
-- Name: fornitori; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.fornitori (
    id_fornitore integer NOT NULL,
    nome character varying(150) NOT NULL,
    telefono character varying(30),
    email character varying(150),
    indirizzo text
);


ALTER TABLE public.fornitori OWNER TO postgres;

--
-- TOC entry 225 (class 1259 OID 24612)
-- Name: fornitori_id_fornitore_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.fornitori_id_fornitore_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.fornitori_id_fornitore_seq OWNER TO postgres;

--
-- TOC entry 5176 (class 0 OID 0)
-- Dependencies: 225
-- Name: fornitori_id_fornitore_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.fornitori_id_fornitore_seq OWNED BY public.fornitori.id_fornitore;


--
-- TOC entry 244 (class 1259 OID 32820)
-- Name: locker; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.locker (
    id_locker integer NOT NULL,
    codice character varying(20) NOT NULL,
    occupato boolean DEFAULT false NOT NULL
);


ALTER TABLE public.locker OWNER TO postgres;

--
-- TOC entry 243 (class 1259 OID 32819)
-- Name: locker_id_locker_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.locker_id_locker_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.locker_id_locker_seq OWNER TO postgres;

--
-- TOC entry 5177 (class 0 OID 0)
-- Dependencies: 243
-- Name: locker_id_locker_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.locker_id_locker_seq OWNED BY public.locker.id_locker;


--
-- TOC entry 233 (class 1259 OID 24683)
-- Name: magazzino; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.magazzino (
    id_magazzino integer NOT NULL,
    id_prodotto character varying(20) NOT NULL,
    quantita_disponibile integer NOT NULL,
    id_ultimo_riordino_arrivato integer
);


ALTER TABLE public.magazzino OWNER TO postgres;

--
-- TOC entry 232 (class 1259 OID 24682)
-- Name: magazzino_id_magazzino_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.magazzino_id_magazzino_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.magazzino_id_magazzino_seq OWNER TO postgres;

--
-- TOC entry 5178 (class 0 OID 0)
-- Dependencies: 232
-- Name: magazzino_id_magazzino_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.magazzino_id_magazzino_seq OWNED BY public.magazzino.id_magazzino;


--
-- TOC entry 229 (class 1259 OID 24639)
-- Name: ordini; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ordini (
    id_ordine integer NOT NULL,
    id_utente integer NOT NULL,
    data_ordine timestamp without time zone DEFAULT now() NOT NULL,
    stato character varying(20) NOT NULL,
    totale numeric(10,2) NOT NULL,
    metodo_consegna text DEFAULT 'LOCKER'::text,
    id_locker integer,
    codice_ritiro character varying(20),
    indirizzo_spedizione text,
    CONSTRAINT ordini_stato_chk CHECK (((stato)::text = ANY ((ARRAY['CREATO'::character varying, 'SPEDITO'::character varying, 'CONSEGNATO'::character varying, 'ANNULLATO'::character varying, 'CONCLUSO'::character varying])::text[])))
);


ALTER TABLE public.ordini OWNER TO postgres;

--
-- TOC entry 228 (class 1259 OID 24638)
-- Name: ordini_id_ordine_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ordini_id_ordine_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.ordini_id_ordine_seq OWNER TO postgres;

--
-- TOC entry 5179 (class 0 OID 0)
-- Dependencies: 228
-- Name: ordini_id_ordine_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.ordini_id_ordine_seq OWNED BY public.ordini.id_ordine;


--
-- TOC entry 227 (class 1259 OID 24623)
-- Name: prodotti; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.prodotti (
    id_prodotto character varying(20) NOT NULL,
    nome character varying(150) NOT NULL,
    marca character varying(100),
    id_categoria integer NOT NULL,
    descrizione text
);


ALTER TABLE public.prodotti OWNER TO postgres;

--
-- TOC entry 236 (class 1259 OID 24718)
-- Name: prodotti_tag; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.prodotti_tag (
    id_prodotto character varying(20) NOT NULL,
    id_tag integer NOT NULL
);


ALTER TABLE public.prodotti_tag OWNER TO postgres;

--
-- TOC entry 238 (class 1259 OID 24736)
-- Name: righe_ordine; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.righe_ordine (
    id_riga integer NOT NULL,
    id_ordine integer NOT NULL,
    id_prodotto character varying(20) NOT NULL,
    quantita integer NOT NULL,
    prezzo_unitario numeric(10,2) NOT NULL,
    prezzo_totale numeric(10,2) NOT NULL
);


ALTER TABLE public.righe_ordine OWNER TO postgres;

--
-- TOC entry 237 (class 1259 OID 24735)
-- Name: righe_ordine_id_riga_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.righe_ordine_id_riga_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.righe_ordine_id_riga_seq OWNER TO postgres;

--
-- TOC entry 5180 (class 0 OID 0)
-- Dependencies: 237
-- Name: righe_ordine_id_riga_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.righe_ordine_id_riga_seq OWNED BY public.righe_ordine.id_riga;


--
-- TOC entry 231 (class 1259 OID 24658)
-- Name: riordini_magazzino; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.riordini_magazzino (
    id_riordino integer NOT NULL,
    id_prodotto character varying(20) NOT NULL,
    id_fornitore integer NOT NULL,
    quantita_ordinata integer NOT NULL,
    data_ordine timestamp without time zone DEFAULT now() NOT NULL,
    data_arrivo_prevista date,
    data_arrivo_effettiva date,
    arrivato boolean DEFAULT false NOT NULL,
    id_responsabile integer
);


ALTER TABLE public.riordini_magazzino OWNER TO postgres;

--
-- TOC entry 230 (class 1259 OID 24657)
-- Name: riordini_magazzino_id_riordino_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.riordini_magazzino_id_riordino_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.riordini_magazzino_id_riordino_seq OWNER TO postgres;

--
-- TOC entry 5181 (class 0 OID 0)
-- Dependencies: 230
-- Name: riordini_magazzino_id_riordino_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.riordini_magazzino_id_riordino_seq OWNED BY public.riordini_magazzino.id_riordino;


--
-- TOC entry 240 (class 1259 OID 32769)
-- Name: scaffali; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.scaffali (
    id_scaffale integer NOT NULL,
    nome character varying(100) NOT NULL,
    descrizione text
);


ALTER TABLE public.scaffali OWNER TO postgres;

--
-- TOC entry 239 (class 1259 OID 32768)
-- Name: scaffali_id_scaffale_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.scaffali_id_scaffale_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.scaffali_id_scaffale_seq OWNER TO postgres;

--
-- TOC entry 5182 (class 0 OID 0)
-- Dependencies: 239
-- Name: scaffali_id_scaffale_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.scaffali_id_scaffale_seq OWNED BY public.scaffali.id_scaffale;


--
-- TOC entry 242 (class 1259 OID 32786)
-- Name: sovracategorie; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sovracategorie (
    id_sovracategoria integer NOT NULL,
    nome character varying(100) NOT NULL
);


ALTER TABLE public.sovracategorie OWNER TO postgres;

--
-- TOC entry 241 (class 1259 OID 32785)
-- Name: sovracategorie_id_sovracategoria_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sovracategorie_id_sovracategoria_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sovracategorie_id_sovracategoria_seq OWNER TO postgres;

--
-- TOC entry 5183 (class 0 OID 0)
-- Dependencies: 241
-- Name: sovracategorie_id_sovracategoria_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sovracategorie_id_sovracategoria_seq OWNED BY public.sovracategorie.id_sovracategoria;


--
-- TOC entry 224 (class 1259 OID 24604)
-- Name: tag; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tag (
    id_tag integer NOT NULL,
    nome character varying(100) NOT NULL
);


ALTER TABLE public.tag OWNER TO postgres;

--
-- TOC entry 223 (class 1259 OID 24603)
-- Name: tag_id_tag_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.tag_id_tag_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tag_id_tag_seq OWNER TO postgres;

--
-- TOC entry 5184 (class 0 OID 0)
-- Dependencies: 223
-- Name: tag_id_tag_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.tag_id_tag_seq OWNED BY public.tag.id_tag;


--
-- TOC entry 220 (class 1259 OID 24578)
-- Name: utenti; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.utenti (
    id_utente integer NOT NULL,
    nome character varying(100) NOT NULL,
    cognome character varying(100) NOT NULL,
    email character varying(150) NOT NULL,
    telefono character varying(30),
    data_creazione timestamp without time zone DEFAULT now() NOT NULL,
    ruolo character varying(50),
    password character varying(1000)
);


ALTER TABLE public.utenti OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 24577)
-- Name: utenti_id_utente_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.utenti_id_utente_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.utenti_id_utente_seq OWNER TO postgres;

--
-- TOC entry 5185 (class 0 OID 0)
-- Dependencies: 219
-- Name: utenti_id_utente_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.utenti_id_utente_seq OWNED BY public.utenti.id_utente;


--
-- TOC entry 4935 (class 2604 OID 24706)
-- Name: catalogo id_catalogo; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.catalogo ALTER COLUMN id_catalogo SET DEFAULT nextval('public.catalogo_id_catalogo_seq'::regclass);


--
-- TOC entry 4925 (class 2604 OID 24596)
-- Name: categorie_prodotti id_categoria; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categorie_prodotti ALTER COLUMN id_categoria SET DEFAULT nextval('public.categorie_prodotti_id_categoria_seq'::regclass);


--
-- TOC entry 4927 (class 2604 OID 24616)
-- Name: fornitori id_fornitore; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fornitori ALTER COLUMN id_fornitore SET DEFAULT nextval('public.fornitori_id_fornitore_seq'::regclass);


--
-- TOC entry 4939 (class 2604 OID 32823)
-- Name: locker id_locker; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.locker ALTER COLUMN id_locker SET DEFAULT nextval('public.locker_id_locker_seq'::regclass);


--
-- TOC entry 4934 (class 2604 OID 24686)
-- Name: magazzino id_magazzino; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.magazzino ALTER COLUMN id_magazzino SET DEFAULT nextval('public.magazzino_id_magazzino_seq'::regclass);


--
-- TOC entry 4928 (class 2604 OID 24642)
-- Name: ordini id_ordine; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ordini ALTER COLUMN id_ordine SET DEFAULT nextval('public.ordini_id_ordine_seq'::regclass);


--
-- TOC entry 4936 (class 2604 OID 24739)
-- Name: righe_ordine id_riga; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.righe_ordine ALTER COLUMN id_riga SET DEFAULT nextval('public.righe_ordine_id_riga_seq'::regclass);


--
-- TOC entry 4931 (class 2604 OID 24661)
-- Name: riordini_magazzino id_riordino; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.riordini_magazzino ALTER COLUMN id_riordino SET DEFAULT nextval('public.riordini_magazzino_id_riordino_seq'::regclass);


--
-- TOC entry 4937 (class 2604 OID 32772)
-- Name: scaffali id_scaffale; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scaffali ALTER COLUMN id_scaffale SET DEFAULT nextval('public.scaffali_id_scaffale_seq'::regclass);


--
-- TOC entry 4938 (class 2604 OID 32789)
-- Name: sovracategorie id_sovracategoria; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sovracategorie ALTER COLUMN id_sovracategoria SET DEFAULT nextval('public.sovracategorie_id_sovracategoria_seq'::regclass);


--
-- TOC entry 4926 (class 2604 OID 24607)
-- Name: tag id_tag; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tag ALTER COLUMN id_tag SET DEFAULT nextval('public.tag_id_tag_seq'::regclass);


--
-- TOC entry 4923 (class 2604 OID 24581)
-- Name: utenti id_utente; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.utenti ALTER COLUMN id_utente SET DEFAULT nextval('public.utenti_id_utente_seq'::regclass);


--
-- TOC entry 5158 (class 0 OID 24703)
-- Dependencies: 235
-- Data for Name: catalogo; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.catalogo VALUES (8, 'prd-07', 25, 2.49, 2.89, 6);
INSERT INTO public.catalogo VALUES (10, 'prd-09', 18, 4.99, 5.49, 7);
INSERT INTO public.catalogo VALUES (11, 'prd-10', 12, 4.79, 5.29, 7);
INSERT INTO public.catalogo VALUES (14, 'prd-13', 8, 9.99, 10.49, 9);
INSERT INTO public.catalogo VALUES (16, 'prd-15', 16, 1.79, 2.09, 3);
INSERT INTO public.catalogo VALUES (17, 'prd-16', 14, 1.99, 2.29, 2);
INSERT INTO public.catalogo VALUES (18, 'prd-17', 24, 1.49, 1.79, 10);
INSERT INTO public.catalogo VALUES (19, 'prd-18', 30, 1.39, 1.69, 10);
INSERT INTO public.catalogo VALUES (12, 'prd-11', 19, 4.49, 4.99, 8);
INSERT INTO public.catalogo VALUES (9, 'prd-08', 14, 5.99, 6.49, 7);
INSERT INTO public.catalogo VALUES (4, 'prd-01', 0, 2.49, 2.99, 2);
INSERT INTO public.catalogo VALUES (15, 'prd-14', 16, 1.59, 1.89, 3);
INSERT INTO public.catalogo VALUES (20, 'prd-19', 21, 1.29, 1.59, 10);
INSERT INTO public.catalogo VALUES (7, 'prd-06', 24, 2.79, 3.19, 6);
INSERT INTO public.catalogo VALUES (6, 'prd-05', 27, 3.49, 3.99, 5);
INSERT INTO public.catalogo VALUES (13, 'prd-12', 9, 7.99, 8.49, 9);
INSERT INTO public.catalogo VALUES (5, 'prd-04', 16, 1.99, 2.29, 4);
INSERT INTO public.catalogo VALUES (2, 'prd-02', 7, 3.10, 3.49, 1);
INSERT INTO public.catalogo VALUES (21, 'prd-03', 2, 1.99, 2.20, 10);
INSERT INTO public.catalogo VALUES (22, 'prd-02', 1, 3.10, 3.49, 18);
INSERT INTO public.catalogo VALUES (3, 'prd-03', 5, 1.99, 2.20, 3);
INSERT INTO public.catalogo VALUES (1, 'prd-01', 53, 2.49, 2.99, 1);
INSERT INTO public.catalogo VALUES (23, 'prd-05', 2, 3.49, 3.99, 1);
INSERT INTO public.catalogo VALUES (24, 'prd-12', 1, 7.99, 8.49, 8);


--
-- TOC entry 5145 (class 0 OID 24593)
-- Dependencies: 222
-- Data for Name: categorie_prodotti; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.categorie_prodotti VALUES (1, 'Detersivi', 'Prodotti per pulizia e manutenzione', 1);
INSERT INTO public.categorie_prodotti VALUES (2, 'Lampadine', 'Illuminazione domestica', 1);
INSERT INTO public.categorie_prodotti VALUES (3, 'Igiene Orale', 'Prodotti per la cura dei denti', 2);
INSERT INTO public.categorie_prodotti VALUES (4, 'Shampoo e Bagnoschiuma', 'Prodotti per capelli e corpo', 2);
INSERT INTO public.categorie_prodotti VALUES (5, 'Manzo', 'Tagli di carne bovina', 3);
INSERT INTO public.categorie_prodotti VALUES (6, 'Pollo', 'Carni bianche di pollo', 3);
INSERT INTO public.categorie_prodotti VALUES (7, 'Tritato', 'Carne tritata pronta all’uso', 3);
INSERT INTO public.categorie_prodotti VALUES (8, 'Congelato', 'Pesce surgelato', 4);
INSERT INTO public.categorie_prodotti VALUES (9, 'Fresco', 'Pesce fresco da banco', 4);
INSERT INTO public.categorie_prodotti VALUES (10, 'Crudo', 'Pesce per consumo crudo', 4);
INSERT INTO public.categorie_prodotti VALUES (11, 'Zucchine', 'Verdure fresche selezionate', 5);
INSERT INTO public.categorie_prodotti VALUES (12, 'Insalata', 'Vari tipi di insalate', 5);
INSERT INTO public.categorie_prodotti VALUES (13, 'Melanzana', 'Varietà di melanzane fresche', 5);
INSERT INTO public.categorie_prodotti VALUES (14, 'Stagione', 'Frutta di stagione', 6);
INSERT INTO public.categorie_prodotti VALUES (15, 'Esotica', 'Frutta esotica importata', 6);
INSERT INTO public.categorie_prodotti VALUES (16, 'Energetiche', 'Bevande energetiche', 7);
INSERT INTO public.categorie_prodotti VALUES (17, 'Zero', 'Bevande senza zucchero', 7);
INSERT INTO public.categorie_prodotti VALUES (18, 'Tè', 'Tè freddo e infusi', 7);


--
-- TOC entry 5168 (class 0 OID 32837)
-- Dependencies: 245
-- Data for Name: consegna_domicilio; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.consegna_domicilio VALUES (21, NULL, '2025-11-18 20:45:35.546761', NULL);
INSERT INTO public.consegna_domicilio VALUES (19, NULL, '2025-11-18 20:45:35.546761', NULL);
INSERT INTO public.consegna_domicilio VALUES (18, NULL, '2025-11-18 20:45:35.546761', '2025-11-18 20:45:50.546761');
INSERT INTO public.consegna_domicilio VALUES (1, NULL, '2025-11-22 01:35:24.756315', NULL);
INSERT INTO public.consegna_domicilio VALUES (26, NULL, '2025-11-22 09:23:53.197526', NULL);
INSERT INTO public.consegna_domicilio VALUES (23, NULL, '2025-11-22 09:23:53.197526', NULL);
INSERT INTO public.consegna_domicilio VALUES (67, NULL, '2025-11-22 10:01:05.842074', '2025-11-22 10:03:33.183892');


--
-- TOC entry 5149 (class 0 OID 24613)
-- Dependencies: 226
-- Data for Name: fornitori; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.fornitori VALUES (1, 'OrtoVerde Srl', '+390612345001', 'ordini@ortoverde.it', 'Via dei Campi 12, Roma');
INSERT INTO public.fornitori VALUES (2, 'AgrumiSud Spa', '+390892345002', 'vendite@agrumisud.it', 'Contrada Aranci 5, Reggio Calabria');
INSERT INTO public.fornitori VALUES (3, 'Fresco&Co', '+390212345003', 'info@frescoco.it', 'Via Mercato 9, Milano');
INSERT INTO public.fornitori VALUES (4, 'Nordic Fresh GmbH', '+49 30 9988771', 'vendite@nordicfresh.eu', 'Boxhagener Str. 18, Berlino');
INSERT INTO public.fornitori VALUES (5, 'Dolci Colline', '+39 0577 223344', 'ordini@dolcicolline.it', 'Via del Chianti 45, Siena');
INSERT INTO public.fornitori VALUES (6, 'Mediterranea Food', '+39 091 6655443', 'sales@medfood.it', 'Via Calatafimi 12, Palermo');
INSERT INTO public.fornitori VALUES (7, 'AlpiBio Distribuzione', '+39 011 7788990', 'info@alpibio.it', 'Corso Francia 210, Torino');
INSERT INTO public.fornitori VALUES (8, 'GustoZero Srl', '+39 02 4455667', 'acquisti@gustozero.it', 'Via Tortona 7, Milano');


--
-- TOC entry 5167 (class 0 OID 32820)
-- Dependencies: 244
-- Data for Name: locker; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.locker VALUES (2, 'LCK-002', false);
INSERT INTO public.locker VALUES (3, 'LCK-003', false);
INSERT INTO public.locker VALUES (4, 'LCK-004', false);
INSERT INTO public.locker VALUES (5, 'LCK-005', false);
INSERT INTO public.locker VALUES (6, 'LCK-006', false);
INSERT INTO public.locker VALUES (7, 'LCK-007', false);
INSERT INTO public.locker VALUES (8, 'LCK-008', false);
INSERT INTO public.locker VALUES (9, 'LCK-009', false);
INSERT INTO public.locker VALUES (10, 'LCK-010', false);
INSERT INTO public.locker VALUES (11, 'LCK-011', false);
INSERT INTO public.locker VALUES (12, 'LCK-012', false);
INSERT INTO public.locker VALUES (1, 'LCK-001', false);


--
-- TOC entry 5156 (class 0 OID 24683)
-- Dependencies: 233
-- Data for Name: magazzino; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.magazzino VALUES (1, 'prd-01', 140, 10);
INSERT INTO public.magazzino VALUES (5, 'prd-05', 5, 13);
INSERT INTO public.magazzino VALUES (6, 'prd-12', 8, 15);
INSERT INTO public.magazzino VALUES (3, 'prd-03', 50, 3);
INSERT INTO public.magazzino VALUES (2, 'prd-02', 26, 11);
INSERT INTO public.magazzino VALUES (4, 'prd-07', 2, 12);


--
-- TOC entry 5152 (class 0 OID 24639)
-- Dependencies: 229
-- Data for Name: ordini; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.ordini VALUES (2, 2, '2025-11-14 15:45:00', 'CONSEGNATO', 5.97, 'LOCKER', NULL, NULL, '');
INSERT INTO public.ordini VALUES (4, 4, '2025-11-17 11:52:42.91752', 'CREATO', 2.49, 'LOCKER', NULL, NULL, '');
INSERT INTO public.ordini VALUES (5, 4, '2025-11-17 12:01:59.904219', 'CREATO', 7.58, 'LOCKER', NULL, NULL, '');
INSERT INTO public.ordini VALUES (19, 4, '2025-11-18 17:10:56.194375', 'ANNULLATO', 5.99, 'LOCKER', NULL, NULL, '');
INSERT INTO public.ordini VALUES (28, 4, '2025-11-19 21:06:21.625929', 'ANNULLATO', 1.29, 'LOCKER', NULL, NULL, '');
INSERT INTO public.ordini VALUES (27, 4, '2025-11-19 19:10:40.013501', 'ANNULLATO', 6.20, 'LOCKER', NULL, NULL, '');
INSERT INTO public.ordini VALUES (26, 4, '2025-11-19 18:32:06.589632', 'CONCLUSO', 6.20, 'DOMICILIO', NULL, NULL, '');
INSERT INTO public.ordini VALUES (11, 4, '2025-11-17 14:56:36.267859', 'CONCLUSO', 3.98, 'LOCKER', 7, '4OI5VD', '');
INSERT INTO public.ordini VALUES (3, 3, '2025-11-14 18:10:00', 'CONCLUSO', 32.40, 'LOCKER', 8, '7BFSHR', '');
INSERT INTO public.ordini VALUES (22, 4, '2025-11-18 20:46:39.498526', 'CONCLUSO', 3.10, 'LOCKER', 9, 'ATPWXN', '');
INSERT INTO public.ordini VALUES (20, 5, '2025-11-18 17:25:55.107614', 'CONCLUSO', 2.49, 'LOCKER', 10, '4RF1H0', '');
INSERT INTO public.ordini VALUES (17, 5, '2025-11-17 18:53:38.263927', 'CONCLUSO', 4.49, 'LOCKER', 11, 'PE8O3E', '');
INSERT INTO public.ordini VALUES (15, 5, '2025-11-17 15:18:25.293239', 'CONCLUSO', 2.49, 'LOCKER', 12, 'RSWVBQ', '');
INSERT INTO public.ordini VALUES (29, 4, '2025-11-21 00:13:30.352965', 'CONCLUSO', 6.20, 'LOCKER', 7, '7HEM4B', '');
INSERT INTO public.ordini VALUES (25, 4, '2025-11-19 00:53:44.490005', 'CONCLUSO', 3.10, 'LOCKER', 8, 'Z40NDA', '');
INSERT INTO public.ordini VALUES (24, 4, '2025-11-19 00:53:23.951762', 'CONCLUSO', 3.49, 'LOCKER', 9, 'QK8O0A', '');
INSERT INTO public.ordini VALUES (30, 4, '2025-11-21 00:28:00.873844', 'CONCLUSO', 7.47, 'LOCKER', 10, 'SWM277', '');
INSERT INTO public.ordini VALUES (21, 4, '2025-11-18 18:48:20.462345', 'ANNULLATO', 3.18, 'DOMICILIO', NULL, NULL, '');
INSERT INTO public.ordini VALUES (18, 5, '2025-11-17 19:17:54.613428', 'CONCLUSO', 2.49, 'LOCKER', 11, '1HWOAM', '');
INSERT INTO public.ordini VALUES (23, 4, '2025-11-19 00:52:57.431981', 'ANNULLATO', 2.49, 'DOMICILIO', NULL, NULL, '');
INSERT INTO public.ordini VALUES (16, 5, '2025-11-17 17:42:58.703412', 'CONCLUSO', 1.99, 'LOCKER', 12, 'XEVL42', '');
INSERT INTO public.ordini VALUES (13, 4, '2025-11-17 15:12:31.825531', 'CONCLUSO', 4.98, 'LOCKER', 1, 'QA35CL', '');
INSERT INTO public.ordini VALUES (14, 5, '2025-11-17 15:18:17.896057', 'CONCLUSO', 2.49, 'LOCKER', 2, 'WVUZVW', '');
INSERT INTO public.ordini VALUES (12, 4, '2025-11-17 15:10:25.877123', 'ANNULLATO', 4.98, 'LOCKER', NULL, NULL, '');
INSERT INTO public.ordini VALUES (9, 4, '2025-11-17 13:43:32.190967', 'CONCLUSO', 2.49, 'LOCKER', 3, '2JS3T4', '');
INSERT INTO public.ordini VALUES (10, 4, '2025-11-17 14:42:33.832698', 'ANNULLATO', 2.49, 'LOCKER', NULL, NULL, '');
INSERT INTO public.ordini VALUES (7, 5, '2025-11-17 12:20:43.633427', 'ANNULLATO', 5.09, 'LOCKER', NULL, NULL, '');
INSERT INTO public.ordini VALUES (31, 4, '2025-11-21 20:02:03.514249', 'CONCLUSO', 2.79, 'LOCKER', 4, 'BGQE92', '');
INSERT INTO public.ordini VALUES (8, 5, '2025-11-17 13:10:08.016807', 'CONCLUSO', 1.99, 'LOCKER', 5, 'TYF914', '');
INSERT INTO public.ordini VALUES (32, 4, '2025-11-21 23:34:10.194091', 'CONCLUSO', 15.46, 'LOCKER', 6, '3KKF8E', '');
INSERT INTO public.ordini VALUES (6, 4, '2025-11-17 12:02:19.427122', 'ANNULLATO', 1.99, 'LOCKER', NULL, NULL, '');
INSERT INTO public.ordini VALUES (1, 1, '2025-11-13 10:30:00', 'CONCLUSO', 31.10, 'LOCKER', 12, 'RAJTU0', '');
INSERT INTO public.ordini VALUES (33, 4, '2025-11-21 23:55:35.73576', 'CONCLUSO', 3.98, 'LOCKER', 6, 'R8BZLN', '');
INSERT INTO public.ordini VALUES (34, 4, '2025-11-22 02:03:22.308117', 'CONSEGNATO', 3.10, 'LOCKER', NULL, NULL, '');
INSERT INTO public.ordini VALUES (67, 4, '2025-11-22 10:01:05.842074', 'CONSEGNATO', 3.10, 'DOMICILIO', NULL, NULL, 'Via Milone 4, Torino');
INSERT INTO public.ordini VALUES (68, 4, '2025-11-22 10:04:38.080118', 'CONCLUSO', 3.10, 'LOCKER', 7, 'R4Z633', 'Via Milone 4, Torino');
INSERT INTO public.ordini VALUES (69, 4, '2025-11-22 10:20:29.698974', 'CONCLUSO', 1.99, 'LOCKER', 1, 'JOD47X', 'Via Milone 4, Torino');


--
-- TOC entry 5150 (class 0 OID 24623)
-- Dependencies: 227
-- Data for Name: prodotti; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.prodotti VALUES ('prd-01', 'Mele Fuji', 'OrtoVerde', 14, 'Mele Fuji croccanti, confezione da 1kg');
INSERT INTO public.prodotti VALUES ('prd-02', 'Arance Tarocco', 'AgrumiSud', 14, 'Arance Tarocco ricche di Vitamina C');
INSERT INTO public.prodotti VALUES ('prd-03', 'Insalata mista', 'Fresco&Co', 12, 'Insalata mista pronta da condire, busta 250g');
INSERT INTO public.prodotti VALUES ('prd-04', 'Detersivo Piatti Limone', 'CasaPulita', 1, 'Detersivo liquido per piatti al limone, flacone 1L');
INSERT INTO public.prodotti VALUES ('prd-05', 'Lampadina LED 10W', 'LumiLux', 2, 'Lampadina LED 10W attacco E27, luce calda');
INSERT INTO public.prodotti VALUES ('prd-06', 'Dentifricio Sbiancante', 'SmilePlus', 3, 'Dentifricio sbiancante uso quotidiano');
INSERT INTO public.prodotti VALUES ('prd-07', 'Shampoo Delicato 250ml', 'Care&Clean', 4, 'Shampoo delicato per uso frequente');
INSERT INTO public.prodotti VALUES ('prd-08', 'Fettine di Manzo 400g', 'CarniPrime', 5, 'Fettine di manzo selezionato 400g');
INSERT INTO public.prodotti VALUES ('prd-09', 'Petto di Pollo 500g', 'CarniPrime', 6, 'Petto di pollo fresco 500g');
INSERT INTO public.prodotti VALUES ('prd-10', 'Carne Tritata 400g', 'CarniPrime', 7, 'Carne tritata mista 400g');
INSERT INTO public.prodotti VALUES ('prd-11', 'Filetti di Merluzzo Surgelati 400g', 'MareNord', 8, 'Filetti di merluzzo surgelati 400g');
INSERT INTO public.prodotti VALUES ('prd-12', 'Orata Fresca 500g', 'MareNord', 9, 'Orata fresca circa 500g');
INSERT INTO public.prodotti VALUES ('prd-13', 'Salmone per Sushi 200g', 'MareNord', 10, 'Trancio di salmone per sushi 200g');
INSERT INTO public.prodotti VALUES ('prd-14', 'Zucchine chiare 500g', 'OrtoVerde', 11, 'Zucchine chiare fresche 500g');
INSERT INTO public.prodotti VALUES ('prd-15', 'Melanzane lunghe 1kg', 'OrtoVerde', 13, 'Melanzane lunghe fresche 1kg');
INSERT INTO public.prodotti VALUES ('prd-16', 'Mango maturo 1 pezzo', 'Tropico', 15, 'Mango maturo pronto da consumare');
INSERT INTO public.prodotti VALUES ('prd-17', 'Energy Drink 250ml', 'PowerMax', 16, 'Bevanda energetica lattina 250ml');
INSERT INTO public.prodotti VALUES ('prd-18', 'Cola Zero 1.5L', 'BubbleSoft', 17, 'Bibita cola zero zuccheri 1.5L');
INSERT INTO public.prodotti VALUES ('prd-19', 'Tè freddo limone 1.5L', 'FreshTea', 18, 'Tè freddo al limone 1.5L');


--
-- TOC entry 5159 (class 0 OID 24718)
-- Dependencies: 236
-- Data for Name: prodotti_tag; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.prodotti_tag VALUES ('prd-01', 1);
INSERT INTO public.prodotti_tag VALUES ('prd-01', 2);
INSERT INTO public.prodotti_tag VALUES ('prd-02', 3);
INSERT INTO public.prodotti_tag VALUES ('prd-04', 9);
INSERT INTO public.prodotti_tag VALUES ('prd-05', 9);
INSERT INTO public.prodotti_tag VALUES ('prd-06', 8);
INSERT INTO public.prodotti_tag VALUES ('prd-07', 8);
INSERT INTO public.prodotti_tag VALUES ('prd-08', 6);
INSERT INTO public.prodotti_tag VALUES ('prd-09', 6);
INSERT INTO public.prodotti_tag VALUES ('prd-10', 6);
INSERT INTO public.prodotti_tag VALUES ('prd-11', 7);
INSERT INTO public.prodotti_tag VALUES ('prd-11', 6);
INSERT INTO public.prodotti_tag VALUES ('prd-12', 4);
INSERT INTO public.prodotti_tag VALUES ('prd-12', 6);
INSERT INTO public.prodotti_tag VALUES ('prd-13', 3);
INSERT INTO public.prodotti_tag VALUES ('prd-13', 6);
INSERT INTO public.prodotti_tag VALUES ('prd-14', 1);
INSERT INTO public.prodotti_tag VALUES ('prd-15', 1);
INSERT INTO public.prodotti_tag VALUES ('prd-16', 3);
INSERT INTO public.prodotti_tag VALUES ('prd-17', 3);
INSERT INTO public.prodotti_tag VALUES ('prd-18', 5);


--
-- TOC entry 5161 (class 0 OID 24736)
-- Dependencies: 238
-- Data for Name: righe_ordine; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.righe_ordine VALUES (1, 1, 'prd-01', 10, 2.49, 24.90);
INSERT INTO public.righe_ordine VALUES (2, 1, 'prd-02', 2, 3.10, 6.20);
INSERT INTO public.righe_ordine VALUES (3, 2, 'prd-03', 3, 1.99, 5.97);
INSERT INTO public.righe_ordine VALUES (4, 4, 'prd-01', 1, 2.49, 2.49);
INSERT INTO public.righe_ordine VALUES (5, 5, 'prd-01', 1, 2.49, 2.49);
INSERT INTO public.righe_ordine VALUES (6, 5, 'prd-02', 1, 3.10, 3.10);
INSERT INTO public.righe_ordine VALUES (7, 5, 'prd-03', 1, 1.99, 1.99);
INSERT INTO public.righe_ordine VALUES (8, 6, 'prd-03', 1, 1.99, 1.99);
INSERT INTO public.righe_ordine VALUES (9, 7, 'prd-02', 1, 3.10, 3.10);
INSERT INTO public.righe_ordine VALUES (10, 7, 'prd-03', 1, 1.99, 1.99);
INSERT INTO public.righe_ordine VALUES (11, 8, 'prd-03', 1, 1.99, 1.99);
INSERT INTO public.righe_ordine VALUES (12, 9, 'prd-01', 1, 2.49, 2.49);
INSERT INTO public.righe_ordine VALUES (13, 10, 'prd-01', 1, 2.49, 2.49);
INSERT INTO public.righe_ordine VALUES (14, 11, 'prd-03', 2, 1.99, 3.98);
INSERT INTO public.righe_ordine VALUES (15, 12, 'prd-01', 2, 2.49, 4.98);
INSERT INTO public.righe_ordine VALUES (16, 13, 'prd-01', 2, 2.49, 4.98);
INSERT INTO public.righe_ordine VALUES (17, 14, 'prd-01', 1, 2.49, 2.49);
INSERT INTO public.righe_ordine VALUES (18, 15, 'prd-01', 1, 2.49, 2.49);
INSERT INTO public.righe_ordine VALUES (19, 16, 'prd-03', 1, 1.99, 1.99);
INSERT INTO public.righe_ordine VALUES (20, 17, 'prd-11', 1, 4.49, 4.49);
INSERT INTO public.righe_ordine VALUES (21, 18, 'prd-01', 1, 2.49, 2.49);
INSERT INTO public.righe_ordine VALUES (22, 19, 'prd-08', 1, 5.99, 5.99);
INSERT INTO public.righe_ordine VALUES (23, 20, 'prd-01', 1, 2.49, 2.49);
INSERT INTO public.righe_ordine VALUES (24, 21, 'prd-14', 2, 1.59, 3.18);
INSERT INTO public.righe_ordine VALUES (25, 22, 'prd-02', 1, 3.10, 3.10);
INSERT INTO public.righe_ordine VALUES (26, 23, 'prd-01', 1, 2.49, 2.49);
INSERT INTO public.righe_ordine VALUES (27, 24, 'prd-05', 1, 3.49, 3.49);
INSERT INTO public.righe_ordine VALUES (28, 25, 'prd-02', 1, 3.10, 3.10);
INSERT INTO public.righe_ordine VALUES (29, 26, 'prd-02', 2, 3.10, 6.20);
INSERT INTO public.righe_ordine VALUES (30, 27, 'prd-02', 2, 3.10, 6.20);
INSERT INTO public.righe_ordine VALUES (31, 28, 'prd-19', 1, 1.29, 1.29);
INSERT INTO public.righe_ordine VALUES (32, 29, 'prd-02', 2, 3.10, 6.20);
INSERT INTO public.righe_ordine VALUES (33, 30, 'prd-03', 1, 1.99, 1.99);
INSERT INTO public.righe_ordine VALUES (34, 30, 'prd-04', 1, 1.99, 1.99);
INSERT INTO public.righe_ordine VALUES (35, 30, 'prd-05', 1, 3.49, 3.49);
INSERT INTO public.righe_ordine VALUES (36, 31, 'prd-06', 1, 2.79, 2.79);
INSERT INTO public.righe_ordine VALUES (37, 32, 'prd-03', 1, 1.99, 1.99);
INSERT INTO public.righe_ordine VALUES (38, 32, 'prd-04', 1, 1.99, 1.99);
INSERT INTO public.righe_ordine VALUES (39, 32, 'prd-05', 1, 3.49, 3.49);
INSERT INTO public.righe_ordine VALUES (40, 32, 'prd-12', 1, 7.99, 7.99);
INSERT INTO public.righe_ordine VALUES (41, 33, 'prd-04', 2, 1.99, 3.98);
INSERT INTO public.righe_ordine VALUES (42, 34, 'prd-02', 1, 3.10, 3.10);
INSERT INTO public.righe_ordine VALUES (75, 67, 'prd-02', 1, 3.10, 3.10);
INSERT INTO public.righe_ordine VALUES (76, 68, 'prd-02', 1, 3.10, 3.10);
INSERT INTO public.righe_ordine VALUES (77, 69, 'prd-03', 1, 1.99, 1.99);


--
-- TOC entry 5154 (class 0 OID 24658)
-- Dependencies: 231
-- Data for Name: riordini_magazzino; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.riordini_magazzino VALUES (1, 'prd-01', 1, 100, '2025-11-10 09:00:00', '2025-11-12', '2025-11-12', true, 2);
INSERT INTO public.riordini_magazzino VALUES (2, 'prd-02', 2, 80, '2025-11-11 10:15:00', '2025-11-13', NULL, false, 2);
INSERT INTO public.riordini_magazzino VALUES (3, 'prd-03', 3, 50, '2025-11-09 14:30:00', '2025-11-11', '2025-11-11', true, 2);
INSERT INTO public.riordini_magazzino VALUES (4, 'prd-01', 1, 2, '2025-11-16 23:30:22.205855', '2026-01-01', NULL, false, NULL);
INSERT INTO public.riordini_magazzino VALUES (5, 'prd-02', 2, 2, '2025-11-17 01:13:50.891229', NULL, NULL, false, NULL);
INSERT INTO public.riordini_magazzino VALUES (6, 'prd-02', 1, 2, '2025-11-17 11:15:31.808906', NULL, NULL, false, NULL);
INSERT INTO public.riordini_magazzino VALUES (7, 'prd-09', 3, 5, '2025-11-19 22:00:13.97219', '2025-11-19', NULL, false, NULL);
INSERT INTO public.riordini_magazzino VALUES (8, 'prd-01', 1, 4, '2025-11-19 23:24:46.115003', '2025-11-19', NULL, false, NULL);
INSERT INTO public.riordini_magazzino VALUES (9, 'prd-04', 2, 3, '2025-11-21 00:13:18.260816', '2025-11-20', NULL, false, NULL);
INSERT INTO public.riordini_magazzino VALUES (10, 'prd-01', 1, 50, '2025-11-21 20:03:24.90334', '2025-11-21', NULL, true, NULL);
INSERT INTO public.riordini_magazzino VALUES (11, 'prd-02', 2, 3, '2025-11-22 00:01:54.25533', '2025-11-21', NULL, true, NULL);
INSERT INTO public.riordini_magazzino VALUES (12, 'prd-07', 2, 2, '2025-11-23 14:13:02.104156', '2025-11-22', NULL, true, NULL);
INSERT INTO public.riordini_magazzino VALUES (13, 'prd-05', 2, 7, '2025-11-23 14:15:19.965496', '2025-11-22', NULL, true, 2);
INSERT INTO public.riordini_magazzino VALUES (14, 'prd-13', 2, 10, '2025-11-23 15:40:53.281816', '2025-11-22', NULL, false, NULL);
INSERT INTO public.riordini_magazzino VALUES (15, 'prd-12', 2, 9, '2025-11-23 15:50:14.174667', '2025-11-22', NULL, true, NULL);


--
-- TOC entry 5163 (class 0 OID 32769)
-- Dependencies: 240
-- Data for Name: scaffali; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.scaffali VALUES (1, 'Scaffale Frutta 1', 'Zona frutta vicino ingresso');
INSERT INTO public.scaffali VALUES (2, 'Scaffale Frutta 2', 'Zona frutta centrale');
INSERT INTO public.scaffali VALUES (3, 'Scaffale Verdura 1', 'Verdura e insalate');
INSERT INTO public.scaffali VALUES (4, 'Scaffale 4', 'Poligono mappa #4');
INSERT INTO public.scaffali VALUES (5, 'Scaffale 5', 'Poligono mappa #5');
INSERT INTO public.scaffali VALUES (6, 'Scaffale 6', 'Poligono mappa #6');
INSERT INTO public.scaffali VALUES (7, 'Scaffale 7', 'Poligono mappa #7');
INSERT INTO public.scaffali VALUES (8, 'Scaffale 8', 'Poligono mappa #8');
INSERT INTO public.scaffali VALUES (9, 'Scaffale 9', 'Poligono mappa #9');
INSERT INTO public.scaffali VALUES (10, 'Scaffale 10', 'Poligono mappa #10');
INSERT INTO public.scaffali VALUES (11, 'Scaffale 11', 'Poligono mappa #11');
INSERT INTO public.scaffali VALUES (12, 'Scaffale 12', 'Poligono mappa #12');
INSERT INTO public.scaffali VALUES (13, 'Scaffale 13', 'Poligono mappa #13');
INSERT INTO public.scaffali VALUES (14, 'Scaffale 14', 'Poligono mappa #14');
INSERT INTO public.scaffali VALUES (15, 'Scaffale 15', 'Poligono mappa #15');
INSERT INTO public.scaffali VALUES (16, 'Scaffale 16', 'Poligono mappa #16');
INSERT INTO public.scaffali VALUES (17, 'Scaffale 17', 'Poligono mappa #17');
INSERT INTO public.scaffali VALUES (18, 'Scaffale 18', 'Poligono mappa #18');


--
-- TOC entry 5165 (class 0 OID 32786)
-- Dependencies: 242
-- Data for Name: sovracategorie; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.sovracategorie VALUES (1, 'Casa');
INSERT INTO public.sovracategorie VALUES (2, 'Cura Personale');
INSERT INTO public.sovracategorie VALUES (3, 'Carne');
INSERT INTO public.sovracategorie VALUES (4, 'Pesce');
INSERT INTO public.sovracategorie VALUES (5, 'Verdura');
INSERT INTO public.sovracategorie VALUES (6, 'Frutta');
INSERT INTO public.sovracategorie VALUES (7, 'Bevande');


--
-- TOC entry 5147 (class 0 OID 24604)
-- Dependencies: 224
-- Data for Name: tag; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.tag VALUES (1, 'Bio');
INSERT INTO public.tag VALUES (2, 'Dolci');
INSERT INTO public.tag VALUES (3, 'Vitaminico');
INSERT INTO public.tag VALUES (4, 'Produzione Locale');
INSERT INTO public.tag VALUES (5, '0 Zuccheri');
INSERT INTO public.tag VALUES (6, 'Proteico');
INSERT INTO public.tag VALUES (7, 'Sottovuoto');
INSERT INTO public.tag VALUES (8, 'Vegan');
INSERT INTO public.tag VALUES (9, 'Eco Friendly');


--
-- TOC entry 5143 (class 0 OID 24578)
-- Dependencies: 220
-- Data for Name: utenti; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.utenti VALUES (5, 'pier', 'pier', 'pier', NULL, '2025-11-16 21:16:02.060789', 'Cliente', '$2b$10$CwzxRZ5r3HF9UCVGAk0LCuAr7OKURA23nYxsk6TOaIJrPuK8j65Oi');
INSERT INTO public.utenti VALUES (1, 'Mario', 'Rossi', 'mario', '+390612345678', '2025-11-15 13:39:21.504754', 'Cliente', '$2b$10$Y91rLZQ4wsyWuVdBLCQqfuuvruJ1huGIDERtr96GnDP7qtdwUgmp6');
INSERT INTO public.utenti VALUES (2, 'Laura', 'Bianchi', 'laura', '+390212345679', '2025-11-15 13:39:21.504754', 'Responsabile', '$2b$10$Y91rLZQ4wsyWuVdBLCQqfuuvruJ1huGIDERtr96GnDP7qtdwUgmp6');
INSERT INTO public.utenti VALUES (3, 'Giulia', 'Verdi', 'giulia', '+390112345680', '2025-11-15 13:39:21.504754', 'Dipendente', '$2b$10$Y91rLZQ4wsyWuVdBLCQqfuuvruJ1huGIDERtr96GnDP7qtdwUgmp6');
INSERT INTO public.utenti VALUES (4, 'cascett', 'cascione', 'casc', '388', '2025-11-16 18:16:44.962946', 'Cliente', '$2b$10$Y91rLZQ4wsyWuVdBLCQqfuuvruJ1huGIDERtr96GnDP7qtdwUgmp6');


--
-- TOC entry 5186 (class 0 OID 0)
-- Dependencies: 234
-- Name: catalogo_id_catalogo_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.catalogo_id_catalogo_seq', 24, true);


--
-- TOC entry 5187 (class 0 OID 0)
-- Dependencies: 221
-- Name: categorie_prodotti_id_categoria_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.categorie_prodotti_id_categoria_seq', 21, true);


--
-- TOC entry 5188 (class 0 OID 0)
-- Dependencies: 225
-- Name: fornitori_id_fornitore_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.fornitori_id_fornitore_seq', 8, true);


--
-- TOC entry 5189 (class 0 OID 0)
-- Dependencies: 243
-- Name: locker_id_locker_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.locker_id_locker_seq', 6, true);


--
-- TOC entry 5190 (class 0 OID 0)
-- Dependencies: 232
-- Name: magazzino_id_magazzino_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.magazzino_id_magazzino_seq', 6, true);


--
-- TOC entry 5191 (class 0 OID 0)
-- Dependencies: 228
-- Name: ordini_id_ordine_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.ordini_id_ordine_seq', 69, true);


--
-- TOC entry 5192 (class 0 OID 0)
-- Dependencies: 237
-- Name: righe_ordine_id_riga_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.righe_ordine_id_riga_seq', 77, true);


--
-- TOC entry 5193 (class 0 OID 0)
-- Dependencies: 230
-- Name: riordini_magazzino_id_riordino_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.riordini_magazzino_id_riordino_seq', 15, true);


--
-- TOC entry 5194 (class 0 OID 0)
-- Dependencies: 239
-- Name: scaffali_id_scaffale_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.scaffali_id_scaffale_seq', 3, true);


--
-- TOC entry 5195 (class 0 OID 0)
-- Dependencies: 241
-- Name: sovracategorie_id_sovracategoria_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sovracategorie_id_sovracategoria_seq', 7, true);


--
-- TOC entry 5196 (class 0 OID 0)
-- Dependencies: 223
-- Name: tag_id_tag_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.tag_id_tag_seq', 9, true);


--
-- TOC entry 5197 (class 0 OID 0)
-- Dependencies: 219
-- Name: utenti_id_utente_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.utenti_id_utente_seq', 5, true);


--
-- TOC entry 4962 (class 2606 OID 24712)
-- Name: catalogo catalogo_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.catalogo
    ADD CONSTRAINT catalogo_pkey PRIMARY KEY (id_catalogo);


--
-- TOC entry 4948 (class 2606 OID 24602)
-- Name: categorie_prodotti categorie_prodotti_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categorie_prodotti
    ADD CONSTRAINT categorie_prodotti_pkey PRIMARY KEY (id_categoria);


--
-- TOC entry 4978 (class 2606 OID 32843)
-- Name: consegna_domicilio consegna_domicilio_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.consegna_domicilio
    ADD CONSTRAINT consegna_domicilio_pkey PRIMARY KEY (id_ordine);


--
-- TOC entry 4952 (class 2606 OID 24622)
-- Name: fornitori fornitori_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.fornitori
    ADD CONSTRAINT fornitori_pkey PRIMARY KEY (id_fornitore);


--
-- TOC entry 4974 (class 2606 OID 32856)
-- Name: locker locker_codice_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.locker
    ADD CONSTRAINT locker_codice_key UNIQUE (codice);


--
-- TOC entry 4976 (class 2606 OID 32829)
-- Name: locker locker_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.locker
    ADD CONSTRAINT locker_pkey PRIMARY KEY (id_locker);


--
-- TOC entry 4960 (class 2606 OID 24691)
-- Name: magazzino magazzino_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.magazzino
    ADD CONSTRAINT magazzino_pkey PRIMARY KEY (id_magazzino);


--
-- TOC entry 4956 (class 2606 OID 24651)
-- Name: ordini ordini_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ordini
    ADD CONSTRAINT ordini_pkey PRIMARY KEY (id_ordine);


--
-- TOC entry 4954 (class 2606 OID 24632)
-- Name: prodotti prodotti_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prodotti
    ADD CONSTRAINT prodotti_pkey PRIMARY KEY (id_prodotto);


--
-- TOC entry 4964 (class 2606 OID 24724)
-- Name: prodotti_tag prodotti_tag_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prodotti_tag
    ADD CONSTRAINT prodotti_tag_pkey PRIMARY KEY (id_prodotto, id_tag);


--
-- TOC entry 4966 (class 2606 OID 24747)
-- Name: righe_ordine righe_ordine_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.righe_ordine
    ADD CONSTRAINT righe_ordine_pkey PRIMARY KEY (id_riga);


--
-- TOC entry 4958 (class 2606 OID 24671)
-- Name: riordini_magazzino riordini_magazzino_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.riordini_magazzino
    ADD CONSTRAINT riordini_magazzino_pkey PRIMARY KEY (id_riordino);


--
-- TOC entry 4968 (class 2606 OID 32778)
-- Name: scaffali scaffali_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scaffali
    ADD CONSTRAINT scaffali_pkey PRIMARY KEY (id_scaffale);


--
-- TOC entry 4970 (class 2606 OID 32795)
-- Name: sovracategorie sovracategorie_nome_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sovracategorie
    ADD CONSTRAINT sovracategorie_nome_key UNIQUE (nome);


--
-- TOC entry 4972 (class 2606 OID 32793)
-- Name: sovracategorie sovracategorie_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sovracategorie
    ADD CONSTRAINT sovracategorie_pkey PRIMARY KEY (id_sovracategoria);


--
-- TOC entry 4950 (class 2606 OID 24611)
-- Name: tag tag_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tag
    ADD CONSTRAINT tag_pkey PRIMARY KEY (id_tag);


--
-- TOC entry 4944 (class 2606 OID 24591)
-- Name: utenti utenti_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.utenti
    ADD CONSTRAINT utenti_email_key UNIQUE (email);


--
-- TOC entry 4946 (class 2606 OID 24589)
-- Name: utenti utenti_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.utenti
    ADD CONSTRAINT utenti_pkey PRIMARY KEY (id_utente);


--
-- TOC entry 4987 (class 2606 OID 24713)
-- Name: catalogo catalogo_id_prodotto_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.catalogo
    ADD CONSTRAINT catalogo_id_prodotto_fkey FOREIGN KEY (id_prodotto) REFERENCES public.prodotti(id_prodotto);


--
-- TOC entry 4988 (class 2606 OID 32779)
-- Name: catalogo catalogo_id_scaffale_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.catalogo
    ADD CONSTRAINT catalogo_id_scaffale_fkey FOREIGN KEY (id_scaffale) REFERENCES public.scaffali(id_scaffale);


--
-- TOC entry 4993 (class 2606 OID 32844)
-- Name: consegna_domicilio consegna_domicilio_id_ordine_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.consegna_domicilio
    ADD CONSTRAINT consegna_domicilio_id_ordine_fkey FOREIGN KEY (id_ordine) REFERENCES public.ordini(id_ordine) ON DELETE CASCADE;


--
-- TOC entry 4994 (class 2606 OID 32849)
-- Name: consegna_domicilio consegna_domicilio_id_rider_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.consegna_domicilio
    ADD CONSTRAINT consegna_domicilio_id_rider_fkey FOREIGN KEY (id_rider) REFERENCES public.utenti(id_utente);


--
-- TOC entry 4985 (class 2606 OID 24692)
-- Name: magazzino magazzino_id_prodotto_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.magazzino
    ADD CONSTRAINT magazzino_id_prodotto_fkey FOREIGN KEY (id_prodotto) REFERENCES public.prodotti(id_prodotto);


--
-- TOC entry 4986 (class 2606 OID 24697)
-- Name: magazzino magazzino_id_ultimo_riordino_arrivato_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.magazzino
    ADD CONSTRAINT magazzino_id_ultimo_riordino_arrivato_fkey FOREIGN KEY (id_ultimo_riordino_arrivato) REFERENCES public.riordini_magazzino(id_riordino);


--
-- TOC entry 4980 (class 2606 OID 32861)
-- Name: ordini ordini_id_locker_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ordini
    ADD CONSTRAINT ordini_id_locker_fkey FOREIGN KEY (id_locker) REFERENCES public.locker(id_locker) ON UPDATE CASCADE;


--
-- TOC entry 4981 (class 2606 OID 24652)
-- Name: ordini ordini_id_utente_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ordini
    ADD CONSTRAINT ordini_id_utente_fkey FOREIGN KEY (id_utente) REFERENCES public.utenti(id_utente);


--
-- TOC entry 4979 (class 2606 OID 32798)
-- Name: prodotti prodotti_id_categoria_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prodotti
    ADD CONSTRAINT prodotti_id_categoria_fkey FOREIGN KEY (id_categoria) REFERENCES public.categorie_prodotti(id_categoria) ON UPDATE CASCADE;


--
-- TOC entry 4989 (class 2606 OID 24725)
-- Name: prodotti_tag prodotti_tag_id_prodotto_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prodotti_tag
    ADD CONSTRAINT prodotti_tag_id_prodotto_fkey FOREIGN KEY (id_prodotto) REFERENCES public.prodotti(id_prodotto);


--
-- TOC entry 4990 (class 2606 OID 24730)
-- Name: prodotti_tag prodotti_tag_id_tag_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prodotti_tag
    ADD CONSTRAINT prodotti_tag_id_tag_fkey FOREIGN KEY (id_tag) REFERENCES public.tag(id_tag);


--
-- TOC entry 4991 (class 2606 OID 24748)
-- Name: righe_ordine righe_ordine_id_ordine_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.righe_ordine
    ADD CONSTRAINT righe_ordine_id_ordine_fkey FOREIGN KEY (id_ordine) REFERENCES public.ordini(id_ordine);


--
-- TOC entry 4992 (class 2606 OID 24753)
-- Name: righe_ordine righe_ordine_id_prodotto_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.righe_ordine
    ADD CONSTRAINT righe_ordine_id_prodotto_fkey FOREIGN KEY (id_prodotto) REFERENCES public.prodotti(id_prodotto);


--
-- TOC entry 4982 (class 2606 OID 24677)
-- Name: riordini_magazzino riordini_magazzino_id_fornitore_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.riordini_magazzino
    ADD CONSTRAINT riordini_magazzino_id_fornitore_fkey FOREIGN KEY (id_fornitore) REFERENCES public.fornitori(id_fornitore);


--
-- TOC entry 4983 (class 2606 OID 24672)
-- Name: riordini_magazzino riordini_magazzino_id_prodotto_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.riordini_magazzino
    ADD CONSTRAINT riordini_magazzino_id_prodotto_fkey FOREIGN KEY (id_prodotto) REFERENCES public.prodotti(id_prodotto);


--
-- TOC entry 4984 (class 2606 OID 24758)
-- Name: riordini_magazzino riordini_magazzino_id_responsabile_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.riordini_magazzino
    ADD CONSTRAINT riordini_magazzino_id_responsabile_fkey FOREIGN KEY (id_responsabile) REFERENCES public.utenti(id_utente);


-- Completed on 2025-11-23 16:01:08

--
-- PostgreSQL database dump complete
--