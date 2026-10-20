# Webstore — Tietokanta ja REST API

Tämä repositorio sisältää Tietokantaratkaisut-kurssilla toteutetun
tietokanta- ja REST API -projektin. Projektissa rakennettiin
tietokanta ja backend-palvelu kuvitteelliselle verkkokaupalle
käyttäen MariaDB-tietokannanhallintajärjestelmää ja Spring Boot
-ohjelmistokehystä.

REST API -taustapalvelu on tarkoitettu verkkokaupan työntekijöiden
ja ylläpitäjien käyttöön. Se tarjoaa työkalut tuotteiden, tilausten,
asiakkaiden, kategorioiden ja toimittajien hallintaan. Palvelun
kautta voidaan lukea, lisätä, muokata ja poistaa tietoja tietokannasta.

---

## SISÄLTÖ

1. [Teknologiat](#teknologiat)
2. [Aloitus](#aloitus)
3. [Testaus](#testaus)
3. [Tietokannan ominaisuudet](#tietokannan-ominaisuudet)
4. [Sovelluksen tietokantaratkaisut](#sovelluksen-tietokantaratkaisut)
5. [API-päätepisteet](#api-päätepisteet)

---

## TEKNOLOGIAT

| Teknologia | Versio | Tarkoitus |
|------------|--------|-----------|
| Java | 21 | Ohjelmointikieli |
| Spring Boot | 4.1.1 | Web-ohjelmistokehys |
| Spring Data JPA / Hibernate | 7 | Tietokanta-abstraktiokerros |
| MariaDB | 11.5 | Tietokannanhallintajärjestelmä |
| Lombok | - | Boilerplate-koodin vähentäminen |

---

## ALOITUS

### Vaatimukset
- Java 21
- MariaDB 11.5 tai uudempi
- Maven

### Tietokannan käyttöönotto

Aja SQL-tiedostot tässä järjestyksessä:

**1. Luo tietokanta:**

    CREATE DATABASE webstore;

**2. Luo tietokanta, taulut ja testaudata:**

    SOURCE sql/schema_populated_dump.sql;

**3. Luo käyttäjät:**

    SOURCE sql/create_users.sql;

**4. Luo indeksit, näkymät, liipaisimet ja tapahtumat:**

    SOURCE sql/schema_extensions.sql;

SQL-tiedostot kansiossa `sql/`:

| Tiedosto | Sisältö                                                            |
|----------|--------------------------------------------------------------------|
| schema_populated_dump.sql | Tietokannan rakenne ja testaudata                                  |
| create_users.sql | Tietokantakäyttäjät ja oikeudet                                    |
| schema_extensions.sql | Indeksit, näkymät, liipaisimet, tapahtumat, temporaaliominaisuudet |
| queries.sql | SQL-kyselyt ja transaktioesimerkit tehtävistä 1—5 ja 9             |


### REST API:n käynnistys

Konfiguroi tietokantayhteys tiedostossa
src/main/resources/application.yaml. Aseta käyttäjänimeksi
webstore_app ja salasanaksi valitsemasi salasana.

Käynnistä sovellus komennolla:

    mvn clean spring-boot:run

Sovellus käynnistyy osoitteeseen http://localhost:8080

---

## TESTAUS

Projektin toiminta on todennettu kahdella tavalla 
HTTP-testauksella ja yksikkötesteillä.

### API-testaus

Kaikki API-päätepisteet on testattu käytännössä IntelliJ IDEA:n
sisäänrakennetulla HTTP-clientilla. Testit ja niiden vastaukset
löytyvät tiedostosta `test/test.http`. Tiedosto sisältää valmiit
HTTP-pyynnöt jokaiselle päätepisteelle, mukaan lukien onnistuneet
kutsut ja virhetilanteet (esim. riittämätön varasto, olematon
resurssi).

Testatut tilanteet kattavat muun muassa:
- Kaikki CRUD-operaatiot jokaiselle resurssille
- Criteria API -haut eri parametriyhdistelmillä
- JPQL-massapäivitykset
- Optimistisen lukituksen toiminta (version-kentän kasvu)
- Transaktion rollback virhetilanteissa
- Sivutus suurilla tietomäärillä

### JUnit testit

Projektiin on toteutettu JUnit-testejä kriittisimmille
komponenteille: tilauksen luonnin transaktiolle, Criteria API
-haulle ja OrderStatusConverter-muuntimelle. Testit löytyvät
`src/test/java`-kansiosta.

Testit kattavat sekä onnistuneet tilanteet että virhetilanteet,
kuten transaktion rollbackin ja kun varasto ei riitä tai tuotetta
ei löydy. Testeissä käytetään H2-muistitietokantaa erillään
MariaDB-tuotantotietokannasta.

Testit ajetaan komennolla:

    mvn test

Tulos:

    Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
    BUILD SUCCESS

---

## TIETOKANNAN OMINAISUUDET

Tässä osiossa kuvataan tietokantaratkaisun ominaisuudet jotka
eivät näy suoraan API-rajapinnasta mutta ovat olennainen osa
järjestelmän toimintaa ja luotettavuutta.

---

### Tietokantarakenne

Tietokanta koostuu kahdeksasta päätaulusta:

| Taulu | Tarkoitus                                                    |
|-------|--------------------------------------------------------------|
| Customers | Asiakkaiden perustiedot                                      |
| CustomerAddresses | Asiakkaiden toimitusosoitteet (1:1 asiakkaaseen)             |
| Suppliers | Tavarantoimittajat                                           |
| SupplierAddresses | Toimittajien osoitteet (1:M toimittajaan)                    |
| ProductCategories | Tuotekategoriat                                              |
| Products | Tuotteet kategoria- ja toimittajaviittauksineen              |
| Orders | Tilaukset asiakasviittauksineen                              |
| OrderItems | Tilausrivit — N:M-väliitaulu tilausten ja tuotteiden välillä |


Lisäksi tietokannassa on Contacts-taulu yhteystietojen tallentamiseen sekä 
lokitaulu ProductStockLog joka tallentaa
varastosaldon muutoshistorian automaattisesti liipaisimet avulla.

---

### Indeksit

Indeksit on luotu tehostamaan yleisimpiä hakukyselyjä. Indeksien
tarve tunnistettiin EXPLAIN-kyselyjen ja mysqlslap-simulaatioiden
avulla. Simulaatioissa käytettiin 10–50 samanaikaista yhteyttä
ja 100 toistoa per kysely.

| Indeksi | Taulu | Sarake | Tarkoitus |
|---------|-------|--------|-----------|
| idx_name | Products | name | Tuotehaku nimellä |
| idx_price | Products | price | Hintahaarukkahaut |
| idx_price_name | Products | price, name | Kattava indeksi price+name-hauille |
| idx_customeraddress_country | CustomerAddresses | country | Asiakkaiden suodatus maan mukaan |

Merkittävin havainto simulaatioissa oli kysely jossa haettiin
asiakkaita toimittajien maiden perusteella. Alkuperäinen
sisäkysely-versio ei hyödyntänyt indeksiä (rows: 99 822).
Optimoitu JOIN-versio laski hakurivien määrän 386:een.

---

### Transaktiot

Transaktioita käytetään tilanteissa joissa useiden
tietokantaoperaatioiden on onnistuttava yhdessä tai
epäonnistuttava kokonaan.

**Toteutus kahdella tasolla:**

Tietokantatasolla (sql/queries.sql) testattiin transaktioita
SQL:llä — sekä onnistunut tilaus että tahallisesti aiheutettu
virhetilanne ROLLBACKilla. Todennettiin että puolittaisia
muutoksia ei jää tietokantaan.

Spring Boot -tasolla transaktio on toteutettu
`POST /api/orders/complete` -päätepisteessä
@Transactional-annotaatiolla OrderService-luokassa.
Operaatio sisältää kolme vaihetta:
1. Tilauksen luonti Orders-tauluun
2. Tilausrivien luonti OrderItems-tauluun
3. Varastosaldon vähennys jokaiselle tuotteelle

Jos jokin vaihe epäonnistuu, @Transactional peruuttaa
kaikki muutokset automaattisesti. Testattu kahdella
virhetilanteella: tuotetta ei löydy ja varasto ei riitä —
molemmissa tilauksesta ei jäänyt jälkiä tietokantaan.

SQL-esimerkit: sql/queries.sql

---

### Rinnakkaiskäyttö ja lukitus

Tutkittiin kolmea lukitusstrategiaa:

**Automaattinen rivilukitus** — InnoDB lukitsee rivin
automaattisesti UPDATE-operaation ajaksi. Tämä riittää
tilanteissa joissa päivitys tehdään relatiivisesti
(stock_quantity - N), koska tietokanta laskee uuden arvon
itse eikä sovellus lue arvoa ensin.

**Eksplisiittinen lukko (SELECT ... FOR UPDATE)** — lukitsee
rivin jo lukemisvaiheessa. Toinen istunto jää odottamaan
lukon vapautumista. Tämä on tarpeen kun sovellus lukee
arvon, laskee uuden arvon itse ja tallentaa sen takaisin
— ilman lukkoa toinen transaktio voisi muuttaa arvon välissä.

**Optimistinen lukitus (@Version)** — toteutettu
Products-entiteettiin. Tietokantaan lisättiin version-sarake
joka kasvaa automaattisesti jokaisen päivityksen yhteydessä.
Hibernate lisää versionumeron WHERE-lauseeseen jokaisessa
UPDATE-operaatiossa. Jos versio on muuttunut, Hibernate
heittää OptimisticLockException-poikkeuksen. Tämä estää
Lost Update -ongelman tilanteissa joissa kaksi asiakasta
ostaa samaa tuotetta samanaikaisesti.

Johtopäätös: automaattinen rivilukitus riittää useimpiin
tilanteisiin tässä projektissa. Optimistinen lukitus lisättiin
Products-tauluun koska varastosaldo on kriittinen kenttä
johon kohdistuu samanaikaisia päivityksiä.

---

### Näkymät

Tietokantaan on toteutettu kaksi näkymää toistuvien
JOIN-kyselyjen yksinkertaistamiseksi.

**ProductDetails** yhdistää Products- ja ProductCategories-taulut.
Näkymä tarjoaa valmiin tuotelistan kategoriatietoineen ilman
toistuvaa JOIN-kirjoittamista. Näkymä on päivitettävissä
suoraan koska se perustuu yksinkertaiseen kahden taulun liitokseen.

**CategoryProductCount** laskee tuotemäärän kategorioittain
GROUP BY -lauseella. Näkymää käytetään kategoriatilastojen
hakemiseen. Näkymä ei ole päivitettävissä suoraan koska
se sisältää koostefunktion.

Näkymien luontilauseet: sql/schema_extensions.sql

---

### Liipaisimet

Tietokantaan on toteutettu yksi liipaisin varastosaldon
muutosten seurantaan.

**trg_products_stock_change** laukeaa automaattisesti jokaisen
Products-taulun päivityksen jälkeen. Se kirjaa tuotteen id:n,
vanhan varastosaldon, uuden varastosaldon ja muutosajan
ProductStockLog-lokitauluun. Näin kaikki varastosaldon
muutokset ovat jäljitettävissä jälkikäteen.

Liipaisimen toteutus: sql/schema_extensions.sql

---

### Ajastetut tapahtumat

Tietokantaan on toteutettu yksi ajastettu tapahtuma.

**ev_expire_old_orders** tarkistaa minuutin välein onko
tietokannassa tilauksia joiden status on NEW ja jotka ovat
yli kaksi minuuttia vanhoja. Jos löytyy, status päivitetään
automaattisesti EXPIRED-tilaan. Tapahtuma simuloi tilannetta
jossa maksamattomat tilaukset vanhentuvat automaattisesti
ilman manuaalista ylläpitoa. Tuotantoympäristössä aikaväli
olisi esimerkiksi 30 minuuttia.

Ajastetun tapahtuman toteutus: sql/schema_extensions.sql

---

### Temporaaliominaisuudet

Products-taulu käyttää MariaDB:n system versioning
-ominaisuutta. Se tallentaa automaattisesti jokaisen rivin
koko muutoshistorian — hinnan ja varastosaldon muutokset
jäävät pysyvästi talteen ilman erillistä lokitaulua.

Historiallinen tieto haetaan FOR SYSTEM_TIME -syntaksilla
joka on MariaDB:n erityisominaisuus. Voidaan hakea koko
historia, tila tiettynä ajankohtana tai tietyllä aikavälillä
tapahtuneet muutokset.

Käyttötapaukset: hintamuutosten jäljitys, virheellisen
päivityksen palauttaminen ja varastosaldon kehityksen seuranta.

Huomio: system versioning aiheutti rajoituksen tehtävässä 14.
Optimistisen lukituksen version-sarakkeen lisääminen edellytti
system_versioning_alter_history-asetuksen muuttamista KEEP-tilaan
ALTER TABLE -operaation ajaksi.

Temporaalikyselyt: sql/queries.sql

---

### Tietoturva

**Käyttäjäroolit:**

Tietokannassa on kaksi käyttäjää joilla on eri oikeudet.

webstore_admin on ylläpitäjätili jolla on täydet oikeudet
tietokantaan. Sitä käytetään tietokannan hallintaan,
skeemamuutoksiin ja varmuuskopiointiin. Sovellus ei
koskaan yhdistä tällä tunnuksella.

webstore_app on sovelluksen runtime-käyttäjä jolla on
vain luku-, lisäys- ja päivitysoikeudet. Ei poisto-oikeuksia
eikä skeemamuutosoikeuksia. Näin mahdollinen sovellusvirhe
tai SQL-injektio ei voi tuhota tietoja tai muuttaa
tietokannan rakennetta.

**Varmuuskopiointisuunnitelma:**

Varmuuskopiointi tehdään päivittäin mysqldump-komennolla.
Kopiot tallennetaan erilliselle palvelimelle. Seitsemän
vuorokauden kopiot säilytetään. Palautus testataan
erilliseen testikantaan kuukausittain.

Palautuskomento: mysql -u webstore_admin -p webstore
< backup_YYYY-MM-DD.sql

**Skeemamuutoskäytäntö:**

Skeemamuutokset testataan ensin kopiossa tuotantodatasta.
Muutos tehdään ALTER TABLE -komennolla taaksepäin
yhteensopivasti. Onnistuminen tarkistetaan DESCRIBE-komennolla.
Rollback-skripti pidetään valmiina ennen muutosta.

---

## SOVELLUKSEN TIETOKANTARATKAISUT

### JPA-entiteettien suhteet

Tässä osiossa kuvataan JPA-entiteettien väliset suhteet
ja niiden toteutusratkaisut.

**1:M — ProductCategory ja Product**   
Yhdellä kategorialla voi olla monta tuotetta. Kategoriapää
käyttää LAZY-latausta koska tuotelistaa ei tarvita joka
haussa. Tuotepää käyttää EAGER-latausta koska kategorian
nimi halutaan lähes aina tuotteen yhteydessä. CascadeType.ALL
tarkoittaa että kategorian poisto poistaa myös sen tuotteet.

**1:1 — Customer ja CustomerAddress**  
Asiakkaalla on yksi toimitusosoite. CustomerAddress omistaa
suhteen koska vierasavain customer_id on sen taulussa.
Asiakas käyttää EAGER-latausta koska osoite tarvitaan
lähes aina asiakkaan yhteydessä.

**N:M — Order ja Product (OrderItems-väliitaulu)**  
Tilaus voi sisältää monta tuotetta ja tuote voi esiintyä
monessa tilauksessa. Koska OrderItems-taulussa on lisäkenttiä
(quantity, unit_price), tavallinen @ManyToMany ei riitä.
Toteutettiin erillinen OrderItem-entiteetti väliitaululle
ja @EmbeddedId yhdistelmäavaimen mallintamiseen.

**Periytyminen — Customer ja CompanyCustomer**  
Yritysasiakas perii Customer-luokan ja lisää kentät
companyName ja businessId. Strategiana InheritanceType.SINGLE_TABLE
koska lisäkenttiä on vain kaksi — NULL-arvoja syntyy vähän
eikä JOIN-operaatioita tarvita. Discriminator-sarake
customer_type erottaa henkilö- ja yritysasiakkaat.

**N+1-ongelma ja ratkaisu**  
Havaittiin N+1-ongelma GET /api/orders -kutsussa. 
Ratkaistiin kahdella tavalla:
sivutuksella joka rajoittaa palautettavan datan määrää,
ja JOIN FETCH -kyselyllä joka hakee tilaukset ja asiakkaat
yhdellä SQL-kyselyllä. 

---

### JPQL ja criteria API

**JPQL-kyselyt** on toteutettu @Query-annotaatiolla
repositoryissa. Käytetään tilanteissa joissa Spring Data JPA:n
automaattiset metodit eivät riitä — esimerkiksi
massapäivityksissä ja suodatetuissa hauissa.

Toteutetut JPQL-kyselyt:
- Tuotteiden haku hintahaarukalla (ProductRepository)
- Hintojen massapäivitys kategorian perusteella (ProductRepository)
- Varastottomien tuotteiden massapoisto (ProductRepository)
- Vanhojen tilausten merkitseminen EXPIRED-tilaan (OrderRepository)
- Tilausten haku asiakkaiden JOIN FETCHillä (OrderRepository)

**Criteria API** on toteutettu ProductService-luokassa
dynaamista tuotehakua varten. Kaikki kolme hakuparametria
(minPrice, maxPrice, categoryId) ovat valinnaisia — ehdot
lisätään kyselyyn vain jos parametri on annettu. Tätä ei
voi tehdä JPQL:lla koska kyselymerkkijonoa ei voi muuttaa
ajon aikana.

---

### Muuntimet ja tapahtumat

**OrderStatusConverter** muuntaa automaattisesti
Order.status-kentän isoille kirjaimille ennen tietokantaan
tallennusta. Näin API hyväksyy sekä "new" että "NEW" ja
tietokantaan tallennetaan aina yhtenäinen muoto.

**@PrePersist Order-entiteetissä** asettaa orderDate-kentän
automaattisesti jos sitä ei anneta pyynnössä. Lisäksi se
varmistaa että status on aina isoilla kirjaimilla. Toimii
kuten tietokannan liipaisin mutta Java-tasolla.

**@Version Product-entiteetissä** toteuttaa optimistisen
lukituksen. Sarake version kasvaa automaattisesti jokaisessa
päivityksessä. Jos kaksi transaktiota yrittää päivittää
samaa versiota samanaikaisesti, jälkimmäinen saa
HTTP 409 -vastauksen.

### DTO-kerros (Data Transfer Object)

API käyttää DTO-luokkia erottamaan tietokantaentiteetit API-vastauksista. 
DTO määrää tarkasti mitä tietoja palautetaan ja estää tietokantakenttien suoran paljastamisen.

---

## API-PÄÄTEPISTEET

Perusosoite: [http://localhost:8080/api](http://localhost:8080/api)

Kaikki vastaukset ovat JSON-muodossa (paitsi tekstivastaukset
massapäivitys- ja osto-operaatioissa).

| Statuskoodi | Tarkoitus |
|-------------|-----------|
| 200 OK | Onnistunut GET, POST tai PUT |
| 204 No Content | Onnistunut DELETE |
| 400 Bad Request | Virheellinen pyyntö (esim. varasto ei riitä) |
| 404 Not Found | Resurssia ei löydy |

---

API-päätepisteet on ryhmitelty resurssin mukaan. 
Alla on kuvattu kunkin resurssin käytettävissä olevat operaatiot, 
niiden tarkoitus sekä pyyntöjen ja vastausten rakenteet.

- [Tuotteet (Products) — `/api/products`](#tuotteet--apiproducts)
- [Kategoriat (Categories) — `/api/categories`](#kategoriat--apicategories)
- [Asiakkaat (Customers) — `/api/customers`](#asiakkaat--apicustomers)
- [Tilaukset (Orders) — `/api/orders`](#tilaukset--apiorders)
- [Toimittajat (Suppliers) — `/api/suppliers`](#toimittajat--apisuppliers)
- [Kontaktit (Contacts) — `/api/contacts`](#kontaktit--apicontacts)

---

### Tuotteet (Products) — `/api/products`

| Metodi | Päätepiste | Kuvaus |
|--------|------------|--------|
| GET | `/api/products` | Hae kaikki tuotteet |
| GET | `/api/products/{id}` | Hae tuote ID:llä |
| GET | `/api/products/search` | Dynaaminen haku parametreilla |
| POST | `/api/products` | Luo uusi tuote |
| PUT | `/api/products/{id}` | Päivitä tuotteen tiedot |
| PUT | `/api/products/{id}/purchase` | Osta tuote, vähentää varastosaldoa |
| PUT | `/api/products/price-update` | Massapäivitys kategorian hinnoille |
| DELETE | `/api/products/{id}` | Poista tuote |

---

**GET `/api/products`**

Palauttaa kaikki tuotteet listana.

Esimerkkivastaus (200 OK):
```json
[
  {
    "id": 8,
    "name": "Shiny Planet 299",
    "description": "Us really personal yes into yourself task happen.",
    "price": 767.24,
    "stockQuantity": 179,
    "version": 0
  }
]
```

**GET `/api/products/{id}`**

Palauttaa yksittäisen tuotteen tiedot ID:n perusteella.

Parametrit: `id` (kokonaisluku, pakollinen)

Esimerkkivastaus (200 OK):
```json
{
  "id": 8,
  "name": "Shiny Planet 299",
  "description": "Us really personal yes into yourself task happen.",
  "price": 767.24,
  "stockQuantity": 179,
  "version": 0
}
```

**GET `/api/products/search`**

Dynaaminen tuotehaku Criteria API:lla. Kaikki parametrit ovat
valinnaisia — voidaan antaa yksi, kaksi tai kaikki kolme.

Parametrit:
- `minPrice` (desimaaliluku, valinnainen)
- `maxPrice` (desimaaliluku, valinnainen)
- `categoryId` (kokonaisluku, valinnainen)

Esimerkkikutsut: GET `/api/products/search?minPrice=10&maxPrice=50`

Esimerkkivastaus (200 OK):
```json
[
  {
    "id": 644,
    "name": "Eco Shell 611",
    "description": "Your all our score region how effort themselves write.",
    "price": 11.44,
    "stockQuantity": 144,
    "version": 0
  }
]
```

**POST `/api/products`**

Luo uuden tuotteen.

Esimerkkipyyntö:
```json
{
  "name": "Uusi tuote",
  "description": "Kuvaus",
  "price": 29.99,
  "stockQuantity": 100
}
```

**PUT `/api/products/{id}`**

Päivittää tuotteen tiedot. Optimistinen lukitus (@Version) kasvattaa
versionumeroa automaattisesti jokaisen päivityksen yhteydessä.

Parametrit: `id` (kokonaisluku, pakollinen)

Esimerkkipyyntö:
```json
{
  "name": "Updated Product",
  "price": 99.99,
  "stockQuantity": 50
}
```

Esimerkkivastaus (200 OK) — (version kasvu 1):
```json
{
  "id": 4,
  "name": "Updated Product",
  "description": null,
  "price": 99.99,
  "stockQuantity": 50,
  "version": 1
}
```

**PUT `/api/products/{id}/purchase`**

Vähentää tuotteen varastosaldoa. Käyttää optimistista lukitusta
estämään yliostamisen samanaikaisissa ostoissa. Jos varasto ei
riitä, palautetaan 400. Jos samanaikainen päivitys havaitaan,
palautetaan 409.

Parametrit:
- `id` (kokonaisluku, pakollinen)
- `quantity` (kokonaisluku, pakollinen, query-parametri)

Esimerkki: PUT `/api/products/1/purchase?quantity=5`

Esimerkkivastaus (200 OK):

    Purchase successful. New stock: 42

**PUT `/api/products/price-update`**

JPQL-massapäivitys joka nostaa kaikkien tietyn kategorian
tuotteiden hintaa kertoimella. Yksi SQL-lause päivittää
kaikki rivit kerralla ilman silmukkaa.

Parametrit:
- `categoryId` (kokonaisluku, pakollinen)
- `multiplier` (desimaaliluku, pakollinen, esim. 1.1 = +10%)

Esimerkki: PUT `/api/products/price-update?categoryId=1&multiplier=1.1`

Esimerkkivastaus (200 OK):

    Updated 98 products

**DELETE `/api/products/{id}`**

Poistaa tuotteen ID:n perusteella.

Parametrit: `id` (kokonaisluku, pakollinen)

Esimerkkivastaus: (Onnistunut poisto)

    HTTP/1.1 204 No Content

---

### Kategoriat (Categories) — /api/categories

| Metodi | Päätepiste | Kuvaus |
|--------|------------|--------|
| GET | `/api/categories` | Hae kaikki kategoriat tuotteineen |
| GET | `/api/categories/{id}` | Hae kategoria ID:llä |
| PUT | `/api/categories/{id}` | Päivitä kategorian tiedot |
| POST | `/api/categories` | Luo uusi kategoria |
| DELETE | `/api/categories/{id}` | Poista kategoria ja sen tuotteet |

---

**GET `/api/categories`**

Palauttaa kaikki kategoriat ja niihin kuuluvat tuotteet.
Hyödyntää 1:M-assosiaatiota ProductCategory -> Product.

Esimerkkivastaus (200 OK):
```json
[
  {
    "id": 1,
    "name": "Elektroniikka",
    "description": "Sähkölaitteet, komponentit ja elektroniikkatuotteet",
    "products": [
      {
        "id": 7,
        "name": "Magic Bit 840",
        "price": 387.59,
        "stockQuantity": 378,
        "version": 0
      }
    ]
  }
]
```

**GET `/api/categories/{id}`**

Palauttaa yksittäisen kategorian ja siihen kuuluvat tuotteet.

Parametrit: `id` (kokonaisluku, pakollinen)

Esimerkkivastaus (200 OK):
```json
{
  "id": 22,
  "name": "Testikategoria",
  "description": "Testausta varten",
  "products": []
}
```

**POST `/api/categories`**

Luo uuden kategorian.

Esimerkkipyyntö:
```json
{
  "name": "Testikategoria",
  "description": "Testausta varten"
}
```

Esimerkkivastaus (200 OK):
```json
{
  "id": 28,
  "name": "Testikategoria",
  "description": "Testausta varten",
  "products": null
}
```

**PUT `/api/categories/{id}`**

Päivittää kategorian nimen ja kuvauksen.

Parametrit: `id` (kokonaisluku, pakollinen)

Esimerkkipyyntö:
```json
{
  "name": "Päivitetty kategoria",
  "description": "Päivitetty kuvaus"
}
```

Esimerkkivastaus (200 OK):
```json
{
  "id": 22,
  "name": "Päivitetty kategoria",
  "description": "Päivitetty kuvaus",
  "products": []
}
```

**DELETE `/api/categories/{id}`**

Poistaa kategorian. CascadeType.ALL poistaa automaattisesti
myös kaikki kategorian tuotteet. Todennettu: kategoria id 28
poistettiin (204), ja sen jälkeen GET palautti 404.

Parametrit: `id` (kokonaisluku, pakollinen)

Esimerkkivastaus: (Onnistunut poisto)

    HTTP/1.1 204 No Content

---

### Asiakkaat (Customers) — /api/customers

| Metodi | Päätepiste | Kuvaus                             |
|--------|------------|------------------------------------|
| GET | `/api/customers` | Hae kaikki asiakkaat (perustiedot) |
| GET | `/api/customers/{id}` | Hae asiakas osoitteineen           |
| POST | `/api/customers` | Luo henkilöasiakas                 |
| POST | `/api/customers/company` | Luo yritysasiakas                  |
| PUT | `/api/customers/{id}` | Päivitä asiakkaan tiedot           |
| DELETE | `/api/customers/{id}` | Poista asiakas                     |


---

**GET `/api/customers`**

Palauttaa kaikki asiakkaat perustiedoilla (ilman osoitetta ja tilauksia).

Esimerkkivastaus (200 OK):
```json
[
  {
    "id": 1,
    "firstName": "Dustin",
    "lastName": "Carey",
    "email": "vsmith1@example.com",
    "phone": "001-789-824-7188x591"
  }
]
```

**GET `/api/customers/{id}`**

Palauttaa asiakkaan tiedot, toimitusosoitteen ja tilauksen lukumäärän.

Parametrit: `id` (kokonaisluku, pakollinen)

Esimerkkivastaus (200 OK):
```json
{
  "id": 6,
  "firstName": "Christian",
  "lastName": "Meyer",
  "email": "hughesthomas6@example.com",
  "phone": "+1-293-637-2620x1799",
  "streetAddress": "7235 Frederick Neck",
  "postalCode": "78800",
  "city": "New Jonathanmouth",
  "country": "Azerbaijan",
  "orderCount": 2
}
```

**POST `/api/customers`**

Luo uuden henkilöasiakkaan.

Esimerkkipyyntö:
```json
{
  "first_name": "Testi",
  "last_name": "Käyttäjä",
  "email": "testi@example.fi",
  "phone": "040-1111111"
}
```

Esimerkkivastaus (200 OK):
```json
{
  "id": 100003,
  "first_name": "Testi",
  "last_name": "Käyttäjä",
  "email": "testi@example.fi",
  "phone": "040-1111111",
  "address": null,
  "orders": null
}
```

**POST `/api/customers/company`**

Luo yritysasiakkaan. Hyödyntää JPA-periytymistä
(InheritanceType.SINGLE_TABLE). Yritysasiakas tallentuu
samaan Customers-tauluun kuin henkilöasiakas mutta
customer_type-sarake erottaa ne (PERSONAL / COMPANY).

Esimerkkipyyntö:
```json
{
  "firstName": "Matti",
  "lastName": "Meikäläinen",
  "email": "matti@firma.fi",
  "companyName": "Firma Oy",
  "businessId": "1234567-8"
}
```

Esimerkkivastaus (200 OK):
```json
{
  "id": 100002,
  "first_name": "Matti",
  "last_name": "Meikäläinen",
  "email": "matti@firma.fi",
  "phone": null,
  "companyName": "Firma Oy",
  "businessId": "1234567-8",
  "address": null,
  "orders": null
}
```

**PUT `/api/customers/{id}`**

Päivittää asiakkaan perustiedot.

Parametrit: `id` (kokonaisluku, pakollinen)

Esimerkkipyyntö:
```json
{
  "first_name": "Päivitetty",
  "last_name": "Käyttäjä",
  "email": "paivitetty@example.fi",
  "phone": "040-9999999"
}
```

Esimerkkivastaus (200 OK):
```json
{
  "id": 100003,
  "firstName": "Päivitetty",
  "lastName": "Käyttäjä",
  "email": "paivitetty@example.fi",
  "phone": "040-9999999"
}
```

**DELETE `/api/customers/{id}`**

Poistaa asiakkaan.

Parametrit: `id` (kokonaisluku, pakollinen)

Esimerkkivastaus: (Onnistunut poisto)

    HTTP/1.1 204 No Content

---

### Tilaukset (Orders) — /api/orders

| Metodi | Päätepiste | Kuvaus |
|--------|------------|--------|
| GET | `/api/orders` | Hae tilaukset sivutettuna |
| GET | `/api/orders/{id}` | Hae tilaus tilausriveineen |
| GET | `/api/orders/with-customers` | Hae tilaukset JOIN FETCHillä sivutettuna |
| POST | `/api/orders` | Luo uusi tilaus |
| POST | `/api/orders/complete` | Luo täydellinen tilaus transaktiolla |
| PUT | `/api/orders/{id}` | Päivitä tilauksen status ja toimituspäivä |
| DELETE | `/api/orders/{id}` | Poista tilaus ja sen tilausrivit |


---

**GET `/api/orders`**

Palauttaa tilaukset sivutettuna. Sivutus on tärkeä koska
tietokannassa on 200 000+ tilausta. 

Parametrit:
- `page` (kokonaisluku, oletusarvo 0)
- `size` (kokonaisluku, oletusarvo 20)

Esimerkki: GET `/api/orders?page=0&size=20`

Esimerkkivastaus (200 OK) — sisältää sivutusmetatiedot:
```json
{
  "content": [
    {
      "id": 1,
      "orderDate": "2024-04-03T17:51:08",
      "deliveryDate": "2024-04-06T16:16:43",
      "status": "CANCELLED",
      "orderItems": [
        {
          "quantity": 2,
          "unitPrice": 736.44,
          "product": {
            "id": 426,
            "name": "Nova Blade 383",
            "price": 736.44,
            "stockQuantity": 175
          }
        }
      ]
    }
  ],
  "totalElements": 200004,
  "totalPages": 10001,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

**GET `/api/orders/{id}`**

Palauttaa yksittäisen tilauksen asiakastietoineen ja
tilausriveineen. Hyödyntää N:M-assosiaatiota
OrderItems-väliitaulun kautta (order_id + product_id
yhdistelmäavain @EmbeddedId:llä).

Parametrit: `id` (kokonaisluku, pakollinen)

Esimerkkivastaus (200 OK):
```json
{
  "id": 1,
  "orderDate": "2024-04-03T17:51:08",
  "deliveryDate": "2024-04-06T16:16:43",
  "status": "CANCELLED",
  "orderItems": [
    {
      "quantity": 2,
      "unitPrice": 736.44,
      "product": {
        "id": 426,
        "name": "Nova Blade 383",
        "price": 736.44,
        "stockQuantity": 175
      }
    }
  ]
}
```

**POST `/api/orders`**

Luo uuden tilauksen. Kaksi automaattista toimintoa:

OrderStatusConverter muuntaa status-kentän isoille
kirjaimille — "new" tallentuu muodossa "NEW".

@PrePersist asettaa orderDate-kentän automaattisesti
jos sitä ei anneta pyynnössä.

Esimerkkipyyntö ilman orderDatea:
```json
{
  "customer": { "id": 1 }
}
```

Esimerkkivastaus — orderDate asettui automaattisesti,
status oletuksena "NEW":
```json
{
  "id": 200046,
  "orderDate": "2026-10-18T11:39:11.200517",
  "deliveryDate": null,
  "status": "NEW",
  "orderItems": null
}
```

**POST `/api/orders/complete`**

Luo täydellinen tilaus transaktiolla. Operaatio on atominen —
kaikki vaiheet onnistuvat yhdessä tai koko tilaus peruutetaan.

Jos jokin vaihe epäonnistuu (tuotetta ei löydy, varasto ei riitä),
@Transactional peruuttaa kaikki muutokset automaattisesti.

Esimerkkipyyntö:
```json
{
  "customerId": 1,
  "items": [
    { "productId": 10, "quantity": 2 },
    { "productId": 20, "quantity": 1 }
  ]
}
```

Esimerkkivastaus (200 OK):
```json
{
  "id": 200055,
  "orderDate": "2026-10-20T11:48:35.537819",
  "status": "NEW",
  "orderItems": [
    {
      "quantity": 2,
      "unitPrice": 60.49,
      "product": {
        "id": 10,
        "name": "Funky Cube 381",
        "price": 60.49,
        "stockQuantity": 98,
        "version": 1
      }
    }
  ]
}
```

Virhetilanteet:

    Varasto ei riitä (400):

**PUT `/api/orders/{id}`**

Päivittää tilauksen statuksen ja/tai toimituspäivän.
OrderStatusConverter muuntaa statuksen automaattisesti
isoille kirjaimille.

Parametrit: `id` (kokonaisluku, pakollinen)

Esimerkkipyyntö — päivitä vain status:
```json
{
  "status": "cancelled"
}
```

Esimerkkivastaus (200 OK):
```json
{
  "id": 200043,
  "orderDate": "2026-10-17T10:27:33",
  "deliveryDate": null,
  "status": "CANCELLED",
  "orderItems": []
}
```

Esimerkkipyyntö — päivitä status ja toimituspäivä:
```json
{
  "status": "delivered",
  "deliveryDate": "2026-10-20T12:00:00"
}
```

Esimerkkivastaus (200 OK):
```json
{
  "id": 200042,
  "orderDate": "2026-10-17T01:38:24",
  "deliveryDate": "2026-10-20T12:00:00",
  "status": "DELIVERED",
  "orderItems": []
}
```

**DELETE `/api/orders/{id}`**

Poistaa tilauksen. CascadeType.ALL poistaa automaattisesti
myös kaikki tilauksen tilausrivit.

Parametrit: `id` (kokonaisluku, pakollinen)

Esimerkkivastaus: (Onnistunut poisto)

    HTTP/1.1 204 No Content

---

### Toimittajat (Suppliers) — /api/suppliers

| Metodi | Päätepiste | Kuvaus                               |
|--------|------------|--------------------------------------|
| GET | `/api/suppliers` | Hae kaikki toimittajat (perustiedot) |
| GET | `/api/suppliers/{id}` | Hae toimittaja osoitteineen          |
| GET | `/api/suppliers/{id}/products` | Hae toimittajan kaikki tuotteet |
| POST | `/api/suppliers` | Luo uusi toimittaja                  |
| PUT | `/api/suppliers/{id}` | Päivitä toimittajan tiedot           |
| DELETE | `/api/suppliers/{id}` | Poista toimittaja                    |

---

**GET `/api/suppliers`**

Palauttaa kaikki toimittajat perustiedoilla (ilman osoitteita ja tuotteita).

Esimerkkivastaus (200 OK):
```json
[
  {
    "id": 1,
    "name": "Polar Electronics Oy",
    "contactName": "Mia Manninen",
    "phone": "0401001000",
    "email": "mia.manninen@polarelec.fi"
  }
]
```

**GET `/api/suppliers/{id}`**

Palauttaa yksittäisen toimittajan yhteystiedot,
ensimmäisen osoitteen ja toimittajan tuotteiden lukumäärän.

Parametrit: `id` (kokonaisluku, pakollinen)

Esimerkkivastaus (200 OK):
```json
{
  "id": 7,
  "name": "Red Maple Supplies",
  "contactName": "Daniel Cooper",
  "phone": "0445879630",
  "email": "daniel.cooper@redmaple.ca",
  "streetAddress": "Maple Street 587",
  "postalCode": "ON M4B 1B3",
  "city": "Toronto",
  "country": "Canada",
  "productCount": 9
}
```

**GET `/api/suppliers/{id}/products`**

Palauttaa toimittajan kaikki tuotteet kategoriatietoineen.
Erillinen päätepiste koska tuotelista on tarpeen vain
erikseen pyydettäessä.

Parametrit: `id` (kokonaisluku, pakollinen)

Esimerkkivastaus (200 OK):
```json
[
  {
    "id": 29,
    "name": "Amazing Blob 125",
    "description": "Ability simple under value course.",
    "price": 627.62,
    "stockQuantity": 3,
    "version": 0,
    "categoryId": 7,
    "categoryName": "Huonekalut"
  }
]
```

**POST `/api/suppliers`**

Luo uuden toimittajan.

Esimerkkipyyntö:
```json
{
  "name": "Testi Tukku Oy",
  "contactName": "Matti Myyjä",
  "phone": "040-1234567",
  "email": "matti@testitukku.fi"
}
```

Esimerkkivastaus (200 OK):
```json
{
  "id": 103,
  "name": "Testi Tukku Oy",
  "contactName": "Matti Myyjä",
  "phone": "040-1234567",
  "email": "matti@testitukku.fi",
  "addresses": null,
  "products": null
}
```

**PUT `/api/suppliers/{id}`**

Päivittää toimittajan tiedot.

Parametrit: `id` (kokonaisluku, pakollinen)

Esimerkkipyyntö:
```json
{
  "name": "Päivitetty Tukku Oy",
  "contactName": "Matti Myyjä",
  "phone": "040-9999999",
  "email": "uusi@testitukku.fi"
}
```

Esimerkkivastaus (200 OK):
```json
{
  "id": 103,
  "name": "Päivitetty Tukku Oy",
  "contactName": "Matti Myyjä",
  "phone": "040-9999999",
  "email": "uusi@testitukku.fi",
  "addresses": [],
  "products": []
}
```

**DELETE `/api/suppliers/{id}`**

Poistaa toimittajan.

Parametrit: `id` (kokonaisluku, pakollinen)

Esimerkkivastaus: (Onnistunut poisto)

    HTTP/1.1 204 No Content

---

### Kontaktit (Contacts) — `/api/contacts`

| Metodi | Päätepiste | Kuvaus |
|--------|------------|--------|
| GET | `/api/contacts` | Hae kaikki kontaktit |
| GET | `/api/contacts/{id}` | Hae kontakti ID:n perusteella |
| GET | `/api/contacts/email/{email}` | Hae kontaktit sähköpostiosoitteen perusteella |
| POST | `/api/contacts` | Luo uusi kontakti |
| DELETE | `/api/contacts/{id}` | Poista kontakti |

---

**GET `/api/contacts`**

Palauttaa kaikki kontaktit.

Esimerkkivastaus (200 OK):

```json
[
  {
    "email": "hwilliams5002@example.org",
    "id": 1,
    "reference": "316e8f76f94b5b1dd8372d78fe7f67ea"
  }
]
```

**GET `/api/contacts/{id}`**

Palauttaa yksittäisen kontaktin ID:n perusteella.

Parametrit: `id` (kokonaisluku, pakollinen)

Esimerkkivastaus (200 OK):

```json
{
  "email": "brian7396735@example.com",
  "id": 13,
  "reference": "1b18fabd7a95c4d99d4ca5c6d1ddfb62"
}
```

**GET `/api/contacts/email/{email}`**

Palauttaa kaikki kontaktit, jotka vastaavat annettua sähköpostiosoitetta. Samalla sähköpostiosoitteella voi olla useita kontakteja.

Parametrit: `email` (sähköpostiosoite, pakollinen)

Esimerkkivastaus (200 OK):

```json
[
  {
    "email": "miguelbaird34283@example.org",
    "id": 11,
    "reference": "6820e161785052a6ce990bf66fb5b681"
  },
  {
    "email": "miguelbaird34283@example.org",
    "id": 6159,
    "reference": "19fdceb2c4838078d060beb33cf82bf6"
  }
]
```

**POST `/api/contacts`**

Luo uuden kontaktin. `reference` generoidaan automaattisesti.

Esimerkkipyyntö:

```json
{
  "email": "uusi@example.fi"
}
```

Esimerkkivastaus (200 OK):

```json
{
  "email": "uusi@example.fi",
  "id": 500002,
  "reference": "fe4e3fd104704ab1a7b73ef1c0a13047"
}
```

**DELETE `/api/contacts/{id}`**

Poistaa kontaktin ID:n perusteella.

Parametrit: `id` (kokonaisluku, pakollinen)

Esimerkkivastaus:

    HTTP/1.1 204 No Content