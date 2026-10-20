-- ============================================================
-- WEBSTORE — SQL-KYSELYT JA TRANSAKTIOT
-- Tehtävissä 1-5 toteutetut kyselyt ja esimerkit
-- ============================================================


-- ============================================================
-- PERUSHAKUKYSELYT (Tehtävä 1)
-- ============================================================

-- Tuotteet joiden varasto alle 5 kpl, hinnan mukaan järjestettynä
SELECT name, price, stock_quantity
FROM Products
WHERE stock_quantity < 5
ORDER BY price;

-- Kolmen taulun sisäliitos: tuote + kategoria + toimittaja
SELECT Products.name AS product_name,
       Products.price,
       ProductCategories.name AS category_name,
       Suppliers.name AS supplier_name
FROM Products
INNER JOIN ProductCategories ON Products.category_id = ProductCategories.id
INNER JOIN Suppliers ON Suppliers.id = Products.supplier_id;

-- Ulkoliitos: kaikki asiakkaat tilauksineeen, myös ilman tilauksia
SELECT first_name, last_name,
       Orders.id AS order_id,
       Orders.delivery_date
FROM Customers
LEFT JOIN Orders ON Customers.id = Orders.customer_id;

-- GROUP BY + HAVING: kategoriat joissa vähintään 3 tuotetta
SELECT category_id, COUNT(*) AS tuotteiden_maara
FROM Products
GROUP BY category_id
HAVING COUNT(*) >= 3;

-- Sisäkysely: asiakkaat jotka eivät ole koskaan tilanneet
SELECT first_name, last_name
FROM Customers
WHERE id NOT IN (
    SELECT customer_id FROM Orders
);


-- ============================================================
-- INDEKSIEN TESTAUS (Tehtävä 2)
-- ============================================================

-- Indeksin käyttö nimihaussa (LIKE-operaattori)
EXPLAIN SELECT name, price FROM Products WHERE name LIKE 'Kahvi%';

-- Kattava indeksi — Extra: Using index (ei tauluhakua)
EXPLAIN SELECT price FROM Products;

-- Havainto: optimoija ei aina käytä indeksiä pienellä taululla
EXPLAIN SELECT name, price FROM Products WHERE price > 50;

-- Indeksi toimii suoralla vertailulla
EXPLAIN SELECT city, country FROM CustomerAddresses WHERE country = 'Suomi';

-- Havainto: sisäkyselyssä indeksiä EI käytetty
EXPLAIN SELECT city, country FROM CustomerAddresses
WHERE country IN (SELECT country FROM SupplierAddresses);


-- ============================================================
-- KYSELYN OPTIMOINTI (Tehtävä 3)
-- ============================================================

-- Alkuperäinen hidas versio (rows: 99 822, ei käyttänyt indeksiä)
SELECT city, country FROM CustomerAddresses
WHERE country IN (SELECT country FROM SupplierAddresses);

-- Optimoitu JOIN-versio (rows: 386, indeksi käytössä)
SELECT DISTINCT CustomerAddresses.city, CustomerAddresses.country
FROM CustomerAddresses
INNER JOIN SupplierAddresses
    ON CustomerAddresses.country = SupplierAddresses.country;


-- ============================================================
-- TRANSAKTIOT (Tehtävä 4)
-- ============================================================

-- Onnistunut transaktio: tilauksen tekeminen
-- Takaa että kaikki kolme operaatiota onnistuvat yhdessä tai ei ollenkaan
START TRANSACTION;

INSERT INTO Orders (customer_id, status)
VALUES (3, 'NEW');

SET @new_order_id = LAST_INSERT_ID();

INSERT INTO OrderItems (order_id, product_id, quantity, unit_price)
VALUES (@new_order_id, 5, 2, (SELECT price FROM Products WHERE id = 5));

UPDATE Products
SET stock_quantity = stock_quantity - 2
WHERE id = 5;

COMMIT;

-- Epäonnistuva transaktio: ROLLBACKin testaus
-- Tahallinen viite-eheysvirhe (product_id 99999 ei ole olemassa)
-- Varmistaa että puolittaisia muutoksia ei jää tietokantaan
START TRANSACTION;

INSERT INTO Orders (customer_id, status) VALUES (3, 'NEW');
SET @new_order_id = LAST_INSERT_ID();

-- Tämä aiheuttaa ERROR 1452: foreign key constraint fails
INSERT INTO OrderItems (order_id, product_id, quantity, unit_price)
VALUES (@new_order_id, 99999, 2, 10.00);

ROLLBACK;

-- Varmistus: tilausmäärä ei muuttunut
SELECT COUNT(*) FROM Orders WHERE customer_id = 3;
SELECT stock_quantity FROM Products WHERE id = 5;


-- ============================================================
-- RINNAKKAISKÄYTTÖ JA LUKITUS (Tehtävä 5)
-- ============================================================

-- Testi A: automaattinen rivilukitus
-- Aja kahdesta eri istunnosta samanaikaisesti

-- Istunto B:
START TRANSACTION;
SELECT stock_quantity FROM Products WHERE id = 5;
UPDATE Products SET stock_quantity = stock_quantity - 2 WHERE id = 5;
COMMIT;

-- Istunto A:
START TRANSACTION;
SELECT stock_quantity FROM Products WHERE id = 5;
UPDATE Products SET stock_quantity = stock_quantity - 3 WHERE id = 5;
COMMIT;
-- Havainto: InnoDB:n automaattinen rivilukitus esti ristiriidan
-- koska molemmat käyttivät relatiivista laskentaa (stock_quantity - N)

-- Testi B: eksplisiittinen SELECT ... FOR UPDATE -lukko
-- Lukitsee rivin jo lukemisvaiheessa

-- Istunto A (aloita ensin):
START TRANSACTION;
SELECT stock_quantity FROM Products WHERE id = 5 FOR UPDATE;
-- Rivi on nyt lukittu — istunto B jää odottamaan

-- Istunto B (aja samaan aikaan):
START TRANSACTION;
SELECT stock_quantity FROM Products WHERE id = 5 FOR UPDATE;
-- Jää odottamaan kunnes istunto A committaa

-- Istunto A jatkaa ja committaa:
UPDATE Products SET stock_quantity = stock_quantity - 3 WHERE id = 5;
COMMIT;
-- Nyt istunto B vapautuu ja saa tuoreen arvon


-- ============================================================
-- TEMPORAALIKYSELYT (Tehtävä 9)
-- ============================================================

-- Tuotteen koko muutoshistoria
SELECT id, name, price, stock_quantity, row_start, row_end
FROM Products FOR SYSTEM_TIME ALL
WHERE id = 1
ORDER BY row_start;

-- Tuotteen tila tiettynä ajankohtana
SELECT id, name, price, stock_quantity, row_start, row_end
FROM Products FOR SYSTEM_TIME
    AS OF TIMESTAMP '2026-09-16 18:38:55'
WHERE id = 1;

-- Viimeisen 7 päivän muutokset
SELECT id, name, price, stock_quantity, row_start
FROM Products FOR SYSTEM_TIME
    BETWEEN (NOW() - INTERVAL 7 DAY) AND NOW()
WHERE id = 1
ORDER BY row_start;