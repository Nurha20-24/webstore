# Tietokanta projekti


### API-spesifikaatio

#### ProductCategories
GET    /api/categories        - Listaa kaikki tuotekategoriat
GET    /api/categories/{id}   - Hakee yhden kategorian id:n perusteella
POST   /api/categories        - Luo uuden kategorian (nimi + kuvaus)
PUT    /api/categories/{id}   - Päivittää olemassa olevan kategorian
DELETE /api/categories/{id}   - Poistaa kategorian

#### Products
GET    /api/products          - Listaa tuotteet
GET    /api/products/{id}     - Hakee yhden tuotteen tiedot
POST   /api/products          - Lisää uuden tuotteen

#### Customers
GET    /api/customers         - Listaa asiakkaat
POST   /api/orders            - Luo uuden tilauksen
GET    /api/orders/{id}       - Hakee tilauksen tiedot 
