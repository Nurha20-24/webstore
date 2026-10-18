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

osoite: http://localhost:8080/api
