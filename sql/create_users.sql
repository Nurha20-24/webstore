CREATE USER 'webstore_admin'@'localhost' IDENTIFIED BY 'VahvaSalasana123!';

GRANT ALL PRIVILEGES ON webstore.* TO 'webstore_admin'@'localhost';


CREATE USER 'webstore_app'@'localhost' IDENTIFIED BY 'ToinenSalasana456!';

GRANT SELECT, INSERT, UPDATE ON webstore.* TO 'webstore_app'@'localhost';


FLUSH PRIVILEGES;
