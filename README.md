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

Luo ensin tietokanta MariaDB:hen:

    CREATE DATABASE webstore;

Luo sen jälkeen kaksi käyttäjää — ylläpitäjä ja sovelluskäyttäjä.
Ylläpitäjällä on täydet oikeudet tietokantaan. Sovelluskäyttäjällä
on vain luku-, lisäys- ja päivitysoikeudet — ei poisto- eikä
skeemamuutosoikeuksia. Tämä rajoittaa sovellusvirheiden vaikutuksia.

Käyttäjien luontiohjeet: sql/create_users.sql

Aja sen jälkeen tietokantaskripti joka luo taulut, näkymät,
liipaisimet, tapahtumat ja indeksit:

    SOURCE sql/schema.sql;

### REST API:n käynnistys

Konfiguroi tietokantayhteys tiedostossa
src/main/resources/application.yaml. Aseta käyttäjänimeksi
webstore_app ja salasanaksi valitsemasi salasana.

Käynnistä sovellus komennolla:

    mvn clean spring-boot:run

Sovellus käynnistyy osoitteeseen http://localhost:8080

---

## TIETOKANNAN OMINAISUUDET

Tässä osiossa kuvataan tietokantaratkaisun ominaisuudet jotka
eivät näy suoraan API-rajapinnasta mutta ovat olennainen osa
järjestelmän toimintaa ja luotettavuutta.

---

### Tietokantarakenne

Tietokanta koostuu kahdeksasta päätaulusta:

| Taulu | Tarkoitus |
|-------|-----------|
| Customers | Asiakkaiden perustiedot |
| CustomerAddresses | Asiakkaiden toimitusosoitteet (1:1 asiakkaaseen) |
| Suppliers | Tavarantoimittajat |
| SupplierAddresses | Toimittajien osoitteet (1:M toimittajaan) |
| ProductCategories | Tuotekategoriat |
| Products | Tuotteet kategoria- ja toimittajaviittauksineen |
| Orders | Tilaukset asiakasviittauksineen |
| OrderItems | Tilausrivit — N:M-väliitaulu tilausten ja tuotteiden välillä |

Lisäksi tietokannassa on lokitaulu ProductStockLog joka tallentaa
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
epäonnistuttava kokonaan. Puolittaiset muutokset eivät
koskaan jää tietokantaan.

Tunnistetut tilanteet joissa transaktio on välttämätön:

**Tilauksen tekeminen** vaatii kolme operaatiota — tilausrivin
lisäämisen, tilaustuotteiden lisäämisen ja varastosaldon
vähentämisen. Jos esimerkiksi tuotetta ei löydy tai varasto
loppuu kesken, koko tilaus peruutetaan.

**Tilauksen peruutus** vaatii sekä varastosaldon palautuksen
että statuksen päivityksen. Jos vain toinen onnistuu,
järjestelmä jää epäjohdonmukaiseen tilaan.

Spring Boot -sovelluksessa transaktiot on toteutettu
@Transactional-annotaatiolla service-tasolla. Tietokantatasolla
testattiin sekä automaattinen rivilukitus että eksplisiittinen
SELECT ... FOR UPDATE -lukitus.

Transaktioiden SQL-esimerkit: sql/queries.sql

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

Näkymien luontilauseet: sql/schema.sql

---

### Liipaisimet

Tietokantaan on toteutettu yksi liipaisin varastosaldon
muutosten seurantaan.

**trg_products_stock_change** laukeaa automaattisesti jokaisen
Products-taulun päivityksen jälkeen. Se kirjaa tuotteen id:n,
vanhan varastosaldon, uuden varastosaldon ja muutosajan
ProductStockLog-lokitauluun. Näin kaikki varastosaldon
muutokset ovat jäljitettävissä jälkikäteen.

Liipaisimen toteutus: sql/schema.sql

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

Ajastetun tapahtuman toteutus: sql/schema.sql

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

---

## API-PÄÄTEPISTEET

Perusosoite: http://localhost:8080

Kaikki vastaukset ovat JSON-muodossa. Virhetilanteissa palautetaan
HTTP-statuskoodi 404 (resurssia ei löydy) tai 400 (virheellinen pyyntö).
Onnistuneet poisto-operaatiot palauttavat 204 (ei sisältöä).

---

### Tuotteet — /api/products

| Metodi | Päätepiste | Kuvaus |
|--------|------------|--------|
| GET | /api/products | Hae kaikki tuotteet |
| GET | /api/products/{id} | Hae tuote ID:llä |
| GET | /api/products/search | Dynaaminen haku parametreilla |
| POST | /api/products | Luo uusi tuote |
| PUT | /api/products/{id} | Päivitä tuotteen tiedot |
| PUT | /api/products/{id}/purchase | Osta tuote, vähentää varastosaldoa |
| PUT | /api/products/price-update | Massapäivitys kategorian hinnoille |
| DELETE | /api/products/{id} | Poista tuote |

**GET /api/products**

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

**GET /api/products/{id}**

Palauttaa yksittäisen tuotteen tiedot ID:n perusteella.

Parametrit: id (kokonaisluku, pakollinen)

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

**GET /api/products/search**

Dynaaminen tuotehaku Criteria API:lla. Kaikki parametrit ovat
valinnaisia — voidaan antaa yksi, kaksi tai kaikki kolme.

Parametrit:
- minPrice (desimaaliluku, valinnainen)
- maxPrice (desimaaliluku, valinnainen)
- categoryId (kokonaisluku, valinnainen)

Esimerkkikutsut:
GET /api/products/search?minPrice=10&maxPrice=50
GET /api/products/search?categoryId=1
GET /api/products/search?minPrice=10&maxPrice=100&categoryId=1


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

**POST /api/products**

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

**PUT /api/products/{id}**

Päivittää tuotteen tiedot. Optimistinen lukitus (@Version) kasvattaa
versionumeroa automaattisesti jokaisen päivityksen yhteydessä.

Parametrit: id (kokonaisluku, pakollinen)

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

**PUT /api/products/{id}/purchase**

Vähentää tuotteen varastosaldoa. Käyttää optimistista lukitusta
estämään yliostamisen samanaikaisissa ostoissa. Jos varasto ei
riitä, palautetaan 400. Jos samanaikainen päivitys havaitaan,
palautetaan 409.

Parametrit:
- id (kokonaisluku, pakollinen)
- quantity (kokonaisluku, pakollinen, query-parametri)

Esimerkki: PUT /api/products/1/purchase?quantity=5

Esimerkkivastaus (200 OK):

    Purchase successful. New stock: 42


**PUT /api/products/price-update**

JPQL-massapäivitys joka nostaa kaikkien tietyn kategorian
tuotteiden hintaa kertoimella. Yksi SQL-lause päivittää
kaikki rivit kerralla ilman silmukkaa.

Parametrit:
- categoryId (kokonaisluku, pakollinen)
- multiplier (desimaaliluku, pakollinen, esim. 1.1 = +10%)

Esimerkki: PUT /api/products/price-update?categoryId=1&multiplier=1.1

Esimerkkivastaus (200 OK):

    Updated 98 products


---

### Kategoriat — /api/categories

| Metodi | Päätepiste | Kuvaus |
|--------|------------|--------|
| GET | /api/categories | Hae kaikki kategoriat tuotteineen |
| GET | /api/categories/{id} | Hae kategoria ID:llä |
| POST | /api/categories | Luo uusi kategoria |
| DELETE | /api/categories/{id} | Poista kategoria ja sen tuotteet |

**GET /api/categories**

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

**GET /api/categories/{id}**

Palauttaa yksittäisen kategorian ja siihen kuuluvat tuotteet.

Parametrit: id (kokonaisluku, pakollinen)

Esimerkkivastaus (200 OK):
```json
{
  "id": 22,
  "name": "Testikategoria",
  "description": "Testausta varten",
  "products": []
}
```

**POST /api/categories**

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

**DELETE /api/categories/{id}**

Poistaa kategorian. CascadeType.ALL poistaa automaattisesti
myös kaikki kategorian tuotteet. Todennettu: kategoria id 28
poistettiin (204), ja sen jälkeen GET palautti 404.

Parametrit: id (kokonaisluku, pakollinen)

Esimerkkivastaus (Onnistunut poisto):

    HTTP/1.1 204 No Content

---

### Asiakkaat — /api/customers

| Metodi | Päätepiste | Kuvaus |
|--------|------------|--------|
| GET | /api/customers | Hae kaikki asiakkaat |
| GET | /api/customers/{id} | Hae asiakas osoitteineen ja tilauksineen |
| POST | /api/customers | Luo henkilöasiakas |
| POST | /api/customers/company | Luo yritysasiakas |

**GET /api/customers**

Palauttaa kaikki asiakkaat osoitteineen ja tilauksineen.


**GET /api/customers/{id}**

Palauttaa asiakkaan tiedot, toimitusosoitteen (1:1-assosiaatio)
ja tilaushistorian tilausriveineen.

Parametrit: id (kokonaisluku, pakollinen)

Esimerkkivastaus (200 OK):
```json
{
  "id": 1,
  "first_name": "Dustin",
  "last_name": "Carey",
  "email": "vsmith1@example.com",
  "phone": "001-789-824-7188x591",
  "address": {
    "id": 1,
    "streetAddress": "39650 Harrington Plains Suite 474",
    "postalCode": "95942",
    "city": "Larryview",
    "country": "Guadeloupe"
  },
  "orders": [
    {
      "id": 181269,
      "orderDate": "2024-05-02T00:20:53",
      "deliveryDate": "2024-05-08T04:18:07",
      "status": "EXPIRED",
      "orderItems": [
        {
          "quantity": 2,
          "unitPrice": 534.50,
          "product": {
            "id": 183,
            "name": "Turbo Blade 398",
            "price": 534.50
          }
        }
      ]
    }
  ]
}
```

**POST /api/customers**

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

**POST /api/customers/company**

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

---

### Tilaukset — /api/orders

| Metodi | Päätepiste | Kuvaus |
|--------|------------|--------|
| GET | /api/orders | Hae tilaukset sivutettuna |
| GET | /api/orders/{id} | Hae tilaus tilausriveineen |
| GET | /api/orders/with-customers | Hae tilaukset JOIN FETCHillä sivutettuna |
| POST | /api/orders | Luo uusi tilaus |

**GET /api/orders**

Palauttaa tilaukset sivutettuna. Sivutus on tärkeä koska
tietokannassa on 200 000+ tilausta. 

Parametrit:
- page (kokonaisluku, oletusarvo 0)
- size (kokonaisluku, oletusarvo 20)

Esimerkki: GET /api/orders?page=0&size=20

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

**GET /api/orders/{id}**

Palauttaa yksittäisen tilauksen asiakastietoineen ja
tilausriveineen. Hyödyntää N:M-assosiaatiota
OrderItems-väliitaulun kautta (order_id + product_id
yhdistelmäavain @EmbeddedId:llä).

Parametrit: id (kokonaisluku, pakollinen)

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

**POST /api/orders**

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

Esimerkkipyyntö status pienillä kirjaimilla:
```json
{
  "status": "new",
  "customer": { "id": 1 }
}
```

Esimerkkivastaus — muunnin muutti status isoiksi kirjaimiksi:
```json
{
  "id": 200045,
  "orderDate": "2026-10-18T11:38:49.318364",
  "status": "NEW",
  "orderItems": null
}
```

---

### Toimittajat — /api/suppliers

| Metodi | Päätepiste | Kuvaus |
|--------|------------|--------|
| GET | /api/suppliers | Hae kaikki toimittajat |
| GET | /api/suppliers/{id} | Hae toimittaja osoitteineen ja tuotteineen |
| POST | /api/suppliers | Luo uusi toimittaja |
| PUT | /api/suppliers/{id} | Päivitä toimittajan tiedot |
| DELETE | /api/suppliers/{id} | Poista toimittaja |

**GET /api/suppliers**

Palauttaa kaikki toimittajat osoitteineen ja tuotteineen.


Esimerkkivastaus (200 OK):
```json
{
  "addresses": [
    {
      "city": "Rovaniemi",
      "country": "Suomi",
      "id": 1,
      "postalCode": "96400",
      "streetAddress": "Kairatie 5"
    }
  ],
  "contactName": "Mia Manninen",
  "email": "mia.manninen@polarelec.fi",
  "id": 1,
  "name": "Polar Electronics Oy",
  "phone": "0401001000",
  "products": [
    {
      "description": "World yet room provide at skin particular process.",
      "id": 111,
      "name": "Nifty Kit 939",
      "price": 745.53,
      "stockQuantity": 237,
      "version": 0
    }
  ]
}

```

**GET /api/suppliers/{id}**

Palauttaa yksittäisen toimittajan yhteystiedot,
osoitteet ja toimittajan tuotteet.

Parametrit: id (kokonaisluku, pakollinen)

Esimerkkivastaus (200 OK):
```json
{
  "addresses": [
    {
      "city": "Toronto",
      "country": "Canada",
      "id": 7,
      "postalCode": "ON M4B 1B3",
      "streetAddress": "Maple Street 587"
    }
  ],
  "contactName": "Daniel Cooper",
  "email": "daniel.cooper@redmaple.ca",
  "id": 7,
  "name": "Red Maple Supplies",
  "phone": "0445879630",
  "products": [
    {
      "description": "Ability simple under value course.",
      "id": 29,
      "name": "Amazing Blob 125",
      "price": 627.62,
      "stockQuantity": 3,
      "version": 0
    }
  ]
}
```

**POST /api/suppliers**

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

**PUT /api/suppliers/{id}**

Päivittää toimittajan tiedot.

Parametrit: id (kokonaisluku, pakollinen)

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

**DELETE /api/suppliers/{id}**

Poistaa toimittajan.

Parametrit: id (kokonaisluku, pakollinen)

Esimerkkivastaus: (Onnistunut poisto)

    HTTP/1.1 204 No Content