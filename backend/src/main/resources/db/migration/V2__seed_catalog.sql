INSERT INTO categories (name, slug) VALUES
 ('Smartphones','smartphones'),('Laptops','laptops'),('Tablets','tablets'),('Audio','audio'),
 ('Smartwatches','smartwatches'),('Gaming','gaming'),('Accessories','accessories');

INSERT INTO products (category_id, name, brand, description, condition_type, price, original_price, image_url, sold_count) VALUES
 (1,'Galaxy S22 128GB (Refurbished)','Samsung','Certified refurbished. 6.1" AMOLED, 8GB RAM, 128GB storage. Tested battery health above 85%, 6-month ReLoop shop warranty.','REFURBISHED',32999,59999,'/images/products/galaxy-s22.svg',48),
 (1,'iPhone 12 64GB (Refurbished)','Apple','Refurbished iPhone 12 in very good cosmetic condition. New battery fitted, factory reset, 6-month warranty.','REFURBISHED',34999,65900,'/images/products/iphone-12.svg',63),
 (1,'Redmi Note 12 Pro','Xiaomi','Brand new sealed unit. 6.67" AMOLED, 108MP camera, 8GB RAM, 256GB storage.','NEW',23999,27999,'/images/products/redmi-note-12.svg',30),
 (1,'Pixel 6a 128GB (Used)','Google','Used, light wear on frame. Fully functional, original charger not included.','USED',17499,43999,'/images/products/pixel-6a.svg',12),
 (2,'ThinkPad T14 Gen 2 (Refurbished)','Lenovo','Business-class laptop, Ryzen 5 PRO, 16GB RAM, 512GB SSD, 14" FHD. Refurbished and tested.','REFURBISHED',42999,89990,'/images/products/thinkpad-t14.svg',21),
 (2,'MacBook Air M1 256GB (Refurbished)','Apple','13.3" Retina, M1 chip, 8GB RAM, 256GB SSD. Battery cycle count under 200.','REFURBISHED',57999,99900,'/images/products/macbook-air-m1.svg',37),
 (2,'Inspiron 15 3520','Dell','New. Core i5 12th Gen, 16GB RAM, 512GB SSD, 15.6" FHD.','NEW',52990,64990,'/images/products/inspiron-15.svg',9),
 (3,'iPad 9th Gen 64GB (Refurbished)','Apple','10.2" display, A13 Bionic, Wi-Fi. Includes new cable and 6-month warranty.','REFURBISHED',21999,32900,'/images/products/ipad-9.svg',29),
 (3,'Galaxy Tab S6 Lite (Used)','Samsung','10.4" tablet with S Pen, 64GB. Minor scratches on back panel.','USED',14999,36999,'/images/products/tab-s6-lite.svg',8),
 (4,'WH-1000XM4 Headphones (Refurbished)','Sony','Industry-leading noise cancelling. Refurbished with new ear pads.','REFURBISHED',14499,29990,'/images/products/sony-xm4.svg',41),
 (4,'AirPods Pro 2nd Gen','Apple','New, sealed. USB-C charging case with active noise cancellation.','NEW',21999,26900,'/images/products/airpods-pro.svg',55),
 (5,'Apple Watch Series 7 45mm (Refurbished)','Apple','GPS, Always-On Retina display. New strap included.','REFURBISHED',17999,41900,'/images/products/watch-s7.svg',17),
 (5,'Galaxy Watch 5 44mm (Used)','Samsung','Bluetooth variant, good condition, battery health 90%.','USED',9499,28999,'/images/products/galaxy-watch-5.svg',6),
 (6,'PlayStation 5 Disc Edition (Refurbished)','Sony','825GB console with one controller. Tested and cleaned.','REFURBISHED',36999,54990,'/images/products/ps5.svg',33),
 (6,'Nintendo Switch OLED','Nintendo','New. 7" OLED display, 64GB, white Joy-Con.','NEW',28999,32999,'/images/products/switch-oled.svg',26),
 (7,'Logitech MX Master 3S Mouse','Logitech','New. Quiet clicks, 8K DPI sensor, USB-C rechargeable.','NEW',7495,10995,'/images/products/mx-master.svg',44),
 (7,'Anker 65W GaN Charger','Anker','New. Compact 3-port fast charger for laptop, phone and tablet.','NEW',2499,3999,'/images/products/anker-65w.svg',70);

INSERT INTO inventory (product_id, quantity)
SELECT id, CASE WHEN id % 5 = 0 THEN 3 ELSE 10 END FROM products;
