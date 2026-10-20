-- ============================================================
-- WEBSTORE — TIETOKANTALAAJENNUKSET
-- Kurssilla toteutetut rakenteet schema_populated_dump.sql:n päälle
-- Suorita tämä schema_populated_dump.sql:n jälkeen
-- ============================================================


-- ============================================================
-- INDEKSIT (Tehtävä 2)
-- ============================================================

-- Tuotehaku nimellä — yleisin hakutapaus API:ssa
CREATE INDEX idx_name ON Products(name);

-- Hintahaarukkahaut — käytetty /api/products/search-päätepisteessä
CREATE INDEX idx_price ON Products(price);

-- Kattava indeksi (covering index) — price+name-haut ilman tauluhakua
CREATE INDEX idx_price_name ON Products(price, name);

-- Asiakkaiden suodatus maan perusteella
CREATE INDEX idx_customeraddress_country ON CustomerAddresses(country);


-- ============================================================
-- NÄKYMÄT (Tehtävä 6)
-- ============================================================

-- Tuotteet kategoriatietoineen — välttää toistuvan JOIN-kirjoittamisen
-- Päivitettävissä suoraan (yksinkertainen kahden taulun liitos)
CREATE VIEW ProductDetails AS
SELECT Products.name AS product_name,
       Products.price,
       Products.stock_quantity,
       ProductCategories.name AS category_name
FROM Products
INNER JOIN ProductCategories ON Products.category_id = ProductCategories.id;

-- Tuotemäärät kategorioittain — koostenäkymä
-- EI päivitettävissä suoraan (sisältää GROUP BY)
CREATE VIEW CategoryProductCount AS
SELECT ProductCategories.name AS category_name,
       COUNT(*) AS product_count
FROM Products
INNER JOIN ProductCategories ON Products.category_id = ProductCategories.id
GROUP BY ProductCategories.name;


-- ============================================================
-- LOKITAULU JA LIIPAISIN (Tehtävä 7)
-- ============================================================

-- Lokitaulu varastosaldon muutoshistorialle
CREATE TABLE ProductStockLog (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    old_quantity INT NOT NULL,
    new_quantity INT NOT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stocklog_product
        FOREIGN KEY (product_id) REFERENCES Products(id)
);

-- Liipaisin: kirjaa jokaisen varastosaldon muutoksen lokiin automaattisesti
-- Laukeaa AFTER UPDATE — kirjaa vanhan ja uuden arvon
DELIMITER //
CREATE TRIGGER trg_products_stock_change
AFTER UPDATE ON Products
FOR EACH ROW
BEGIN
    INSERT INTO ProductStockLog (product_id, old_quantity, new_quantity)
    VALUES (NEW.id, OLD.stock_quantity, NEW.stock_quantity);
END //
DELIMITER ;


-- ============================================================
-- AJASTETTU TAPAHTUMA (Tehtävä 7)
-- ============================================================

-- Vaaditaan ennen CREATE EVENT -lausetta
SET GLOBAL event_scheduler = ON;

-- Merkitsee maksamattomat NEW-tilaukset automaattisesti EXPIRED-tilaan
-- Tarkistaa minuutin välein onko yli 2 minuuttia vanhoja NEW-tilauksia
-- Tuotannossa aikaväli olisi esim. 30 minuuttia
DELIMITER //
CREATE EVENT ev_expire_old_orders
ON SCHEDULE EVERY 1 MINUTE
DO
BEGIN
    UPDATE Orders
    SET status = 'EXPIRED'
    WHERE status = 'NEW'
      AND order_date < NOW() - INTERVAL 2 MINUTE;
END //
DELIMITER ;


-- ============================================================
-- TEMPORAALIOMINAISUUDET (Tehtävä 9)
-- ============================================================

-- System versioning tallentaa automaattisesti koko muutoshistorian
-- Mahdollistaa hintamuutosten jäljityksen ja virheellisten päivitysten palautuksen
ALTER TABLE Products ADD SYSTEM VERSIONING;


-- ============================================================
-- OPTIMISTINEN LUKITUS (Tehtävä 14)
-- ============================================================

-- version-sarake JPA:n @Version-annotaatiota varten
-- Kasvaa automaattisesti jokaisessa Hibernate-päivityksessä
-- system_versioning_alter_history vaatii KEEP-tilan koska Products käyttää system versioning -ominaisuutta
SET SESSION system_versioning_alter_history = 'KEEP';
ALTER TABLE Products ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
SET SESSION system_versioning_alter_history = 'ERROR';


-- ============================================================
-- SKEEMAMUUTOS ESIMERKKI (Tehtävä 8)
-- ============================================================

-- Esimerkki hallitusta skeemamuutoksesta — uuden sarakkeen lisääminen
ALTER TABLE Products ADD COLUMN discount_percentage DECIMAL(5,2) DEFAULT 0;