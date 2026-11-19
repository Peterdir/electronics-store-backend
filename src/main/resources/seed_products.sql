-- 1. Đảm bảo Categories mẫu tồn tại (status: 'ACTIVE')
INSERT IGNORE INTO `categories` (`id`, `name`, `status`, `created_at`, `updated_at`) VALUES
(1850000000000001001, 'Điện thoại', 'ACTIVE', NOW(), NOW()),
(1850000000000001002, 'Laptop', 'ACTIVE', NOW(), NOW()),
(1850000000000001003, 'Máy tính bảng', 'ACTIVE', NOW(), NOW()),
(1850000000000001004, 'Âm thanh', 'ACTIVE', NOW(), NOW()),
(1850000000000001005, 'Bàn phím & Chuột', 'ACTIVE', NOW(), NOW()),
(1850000000000001006, 'Màn hình', 'ACTIVE', NOW(), NOW()),
(1850000000000001007, 'Đồng hồ thông minh', 'ACTIVE', NOW(), NOW());

-- 2. Đảm bảo Brands mẫu tồn tại (status: 'ACTIVE')
INSERT IGNORE INTO `brands` (`id`, `name`, `status`, `logo`, `created_at`, `updated_at`) VALUES
(1850000000000002001, 'Apple', 'ACTIVE', 'https://placehold.co/200x200/png?text=Apple+Logo', NOW(), NOW()),
(1850000000000002002, 'Samsung', 'ACTIVE', 'https://placehold.co/200x200/png?text=Samsung+Logo', NOW(), NOW()),
(1850000000000002003, 'Dell', 'ACTIVE', 'https://placehold.co/200x200/png?text=Dell+Logo', NOW(), NOW()),
(1850000000000002004, 'Sony', 'ACTIVE', 'https://placehold.co/200x200/png?text=Sony+Logo', NOW(), NOW()),
(1850000000000002005, 'Logitech', 'ACTIVE', 'https://placehold.co/200x200/png?text=Logitech+Logo', NOW(), NOW()),
(1850000000000002006, 'Asus', 'ACTIVE', 'https://placehold.co/200x200/png?text=Asus+Logo', NOW(), NOW()),
(1850000000000002007, 'LG', 'ACTIVE', 'https://placehold.co/200x200/png?text=LG+Logo', NOW(), NOW()),
(1850000000000002008, 'Xiaomi', 'ACTIVE', 'https://placehold.co/200x200/png?text=Xiaomi+Logo', NOW(), NOW()),
(1850000000000002009, 'Marshall', 'ACTIVE', 'https://placehold.co/200x200/png?text=Marshall+Logo', NOW(), NOW()),
(1850000000000002010, 'Lenovo', 'ACTIVE', 'https://placehold.co/200x200/png?text=Lenovo+Logo', NOW(), NOW());

-- 3. Thêm 10 Products (status: 'ACTIVE')
INSERT INTO `products` (`id`, `name`, `base_price`, `description`, `status`, `average_rating`, `specification`, `category_id`, `brand_id`, `created_at`, `updated_at`) VALUES
-- 1. iPhone 18 Pro Max
(
    1850000000000003001,
    'iPhone 18 Pro Max 256GB',
    29990000,
    'Điện thoại flagship cao cấp với khung viền Titanium chuẩn hàng không vũ trụ, chip A17 Pro mạnh mẽ và hệ thống camera 5x telephoto đỉnh cao.',
    'ACTIVE',
    4.9,
    '{"screen": "6.7 inch Super Retina XDR OLED 120Hz", "chip": "Apple A17 Pro (3nm)", "ram": "8GB", "storage": "256GB", "battery": "4422 mAh, Sạc nhanh 20W", "weight": "221g"}',
    1850000000000001001,
    1850000000000002001,
    NOW(),
    NOW()
),
-- 2. Samsung Galaxy S24 Ultra
(
    1850000000000003002,
    'Samsung Galaxy S24 Ultra 256GB',
    27990000,
    'Flagship tích hợp quyền năng Galaxy AI tiên tiến, khung viền Titan sang trọng, bút S-Pen tiện lợi và camera zoom 100x Space Zoom.',
    'ACTIVE',
    4.8,
    '{"screen": "6.8 inch Dynamic AMOLED 2X 120Hz QHD+", "chip": "Snapdragon 8 Gen 3 for Galaxy", "ram": "12GB", "storage": "256GB", "battery": "5000 mAh, Sạc nhanh 45W", "sPen": "Có sẵn"}',
    1850000000000001001,
    1850000000000002002,
    NOW(),
    NOW()
),
-- 3. MacBook Pro 14 M3
(
    1850000000000003003,
    'MacBook Pro 14 inch M3 (8-Core CPU, 10-Core GPU, 18GB RAM, 512GB SSD)',
    39990000,
    'Laptop đồ họa chuyên nghiệp sở hữu chip M3 thế hệ mới, màn hình Liquid Retina XDR sáng 1600 nits và thời lượng pin đột phá lên đến 22 giờ.',
    'ACTIVE',
    4.9,
    '{"screen": "14.2 inch Liquid Retina XDR (3024x1964) 120Hz", "cpu": "Apple M3 (8 nhân CPU, 10 nhân GPU)", "ram": "18GB Unified Memory", "ssd": "512GB NVMe", "battery": "70Wh, sạc MagSafe 3", "ports": "HDMI, SDXC, MagSafe 3, 2x Thunderbolt 4"}',
    1850000000000001002,
    1850000000000002001,
    NOW(),
    NOW()
),
-- 4. Dell XPS 15 9530
(
    1850000000000003004,
    'Dell XPS 15 9530 (Intel Core i7-13700H, 16GB RAM, 512GB SSD, RTX 4050)',
    42500000,
    'Kiệt tác thiết kế nhôm nguyên khối và sợi carbon, cấu hình Intel thế hệ 13 kết hợp GPU NVIDIA RTX 4050 hỗ trợ tối đa cho lập trình viên và creator.',
    'ACTIVE',
    4.7,
    '{"screen": "15.6 inch FHD+ InfinityEdge (1920x1200) 500 nits", "cpu": "Intel Core i7-13700H (14 cores, 20 threads)", "gpu": "NVIDIA GeForce RTX 4050 6GB GDDR6", "ram": "16GB DDR5 4800MHz", "ssd": "512GB PCIe Gen 4 SSD", "weight": "1.86kg"}',
    1850000000000001002,
    1850000000000002003,
    NOW(),
    NOW()
),
-- 5. Sony WH-1000XM5
(
    1850000000000003005,
    'Tai nghe chống ồn không dây Sony WH-1000XM5',
    7990000,
    'Tai nghe trùm đầu chống ồn chủ động (ANC) hàng đầu thế giới với bộ xử lý kép V1/QN1, hỗ trợ âm thanh chuẩn Hi-Res Audio Wireless và LDAC.',
    'ACTIVE',
    4.8,
    '{"driver": "30mm màng loa sợi carbon", "anc": "Chống ồn chủ động kép V1 + QN1 với 8 microphones", "battery": "30 giờ (bật ANC), sạc nhanh 3 phút được 3 giờ", "bluetooth": "Bluetooth 5.2, Codec LDAC, AAC, SBC", "weight": "250g"}',
    1850000000000001004,
    1850000000000002004,
    NOW(),
    NOW()
),
-- 6. iPad Pro 11 inch M4
(
    1850000000000003006,
    'iPad Pro 11 inch M4 Wi-Fi 256GB',
    28990000,
    'Máy tính bảng mỏng nhất từ trước đến nay của Apple (5.3mm), màn hình Ultra Retina XDR Tandem OLED đột phá và sức mạnh vượt trội từ vi xử lý M4.',
    'ACTIVE',
    4.9,
    '{"screen": "11 inch Ultra Retina XDR Tandem OLED 120Hz ProMotion", "chip": "Apple M4 với 16-core Neural Engine", "ram": "8GB", "storage": "256GB", "camera": "12MP Wide + LiDAR Scanner", "thickness": "5.3mm"}',
    1850000000000001003,
    1850000000000002001,
    NOW(),
    NOW()
),
-- 7. Chuột Logitech MX Master 3S
(
    1850000000000003007,
    'Chuột không dây Logitech MX Master 3S',
    2190000,
    'Chuột công thái học cao cấp dành cho chuyên gia với cảm biến 8000 DPI Darkfield lướt trên mọi bề mặt kể cả kính, cuộn siêu tốc MagSpeed và click tĩnh âm 90%.',
    'ACTIVE',
    4.8,
    '{"sensor": "Darkfield 8000 DPI (lướt trên kính)", "clicks": "Quiet Clicks giảm 90% tiếng ồn", "connectivity": "Bluetooth Low Energy & Logi Bolt USB", "battery": "Pin sạc 500mAh dùng 70 ngày", "customization": "Logi Options+ hỗ trợ Flow chuyển đổi 3 thiết bị"}',
    1850000000000001005,
    1850000000000002005,
    NOW(),
    NOW()
),
-- 8. Bàn phím cơ Asus ROG Azoth
(
    1850000000000003008,
    'Bàn phím cơ không dây Asus ROG Azoth 75%',
    5990000,
    'Bàn phím cơ Custom cao cấp layout 75%, trang bị màn hình OLED 2 inch, núm xoay đa năng, đệm Gasket mount 3 lớp tiêu âm và switch ROG NX Red lube sẵn.',
    'ACTIVE',
    4.7,
    '{"layout": "75% (81 phím)", "switch": "ROG NX Red Linear (Hot-swappable)", "display": "Màn hình OLED 2 inch kèm nút điều khiển 3 chiều", "connectivity": "Dây Type-C / 2.4GHz SpeedNova / Bluetooth 5.1", "mount": "Gasket Mount 3 lớp đệm silicone tiêu âm"}',
    1850000000000001005,
    1850000000000002006,
    NOW(),
    NOW()
),
-- 9. Màn hình LG UltraGear OLED 27GR95QE-B
(
    1850000000000003009,
    'Màn hình Gaming LG UltraGear 27GR95QE-B OLED 240Hz 0.03ms',
    18900000,
    'Màn hình chơi game OLED 27 inch chuẩn 2K QHD tốc độ quét 240Hz, thời gian phản hồi 0.03ms (GtG) siêu tốc, độ phủ màu 98.5% DCI-P3 chuẩn điện ảnh.',
    'ACTIVE',
    4.8,
    '{"size": "27 inch OLED (2560 x 1440)", "refreshRate": "240Hz", "responseTime": "0.03ms (GtG)", "colorGamut": "DCI-P3 98.5%", "sync": "NVIDIA G-SYNC Compatible, AMD FreeSync Premium", "hdr": "HDR10"}',
    1850000000000001006,
    1850000000000002007,
    NOW(),
    NOW()
),
-- 10. Apple Watch Series 9 GPS 45mm
(
    1850000000000003010,
    'Đồng hồ thông minh Apple Watch Series 9 GPS 45mm viền nhôm',
    10490000,
    'Smartwatch thế hệ mới nhất với chip S9 SiP mạnh mẽ, tính năng thao tác chạm 2 lần (Double Tap) kỳ diệu, màn hình sáng 2000 nits và theo dõi sức khỏe toàn diện.',
    'ACTIVE',
    4.9,
    '{"caseSize": "45mm viền nhôm, dây cao su thể thao", "screen": "Always-On Retina LTPO OLED, độ sáng 2000 nits", "chip": "Apple S9 SiP 64-bit lõi kép", "health": "Điện tâm đồ ECG, đo nồng độ oxy SpO2, đo nhiệt độ cơ thể", "waterResistance": "Chống nước 50m (WR50)"}',
    1850000000000001007,
    1850000000000002001,
    NOW(),
    NOW()
),
-- 11. Samsung Galaxy Z Fold5 512GB
(
    1850000000000003011,
    'Samsung Galaxy Z Fold5 512GB',
    34990000,
    'Smartphone màn hình gập đỉnh cao với bản lề Flex cải tiến gập không khe hở, màn hình cực đại 7.6 inch Dynamic AMOLED 2X và đa nhiệm thông minh.',
    'ACTIVE',
    4.8,
    '{"mainScreen": "7.6 inch QXGA+ Dynamic AMOLED 2X 120Hz", "coverScreen": "6.2 inch HD+ Dynamic AMOLED 2X 120Hz", "chip": "Snapdragon 8 Gen 2 for Galaxy", "ram": "12GB", "storage": "512GB", "battery": "4400 mAh, sạc nhanh 25W"}',
    1850000000000001001,
    1850000000000002002,
    NOW(),
    NOW()
),
-- 12. Asus ROG Zephyrus G14 (2024)
(
    1850000000000003012,
    'Asus ROG Zephyrus G14 OLED (Ryzen 9 8945HS, 16GB, 1TB SSD, RTX 4060)',
    49990000,
    'Cỗ máy gaming và sáng tạo nội dung siêu mỏng nhẹ với màn hình OLED ROG Nebula 3K 120Hz, chip AMD Ryzen 9 8945HS và GPU NVIDIA RTX 4060.',
    'ACTIVE',
    4.9,
    '{"screen": "14 inch 3K OLED 120Hz 0.2ms 100% DCI-P3", "cpu": "AMD Ryzen 9 8945HS (8 nhân, 16 luồng)", "gpu": "NVIDIA GeForce RTX 4060 8GB GDDR6", "ram": "16GB LPDDR5X", "ssd": "1TB PCIe 4.0 NVMe", "weight": "1.5kg"}',
    1850000000000001002,
    1850000000000002006,
    NOW(),
    NOW()
),
-- 13. Lenovo Legion Pro 5 16IRX8
(
    1850000000000003013,
    'Lenovo Legion Pro 5 16IRX8 (Core i7-13700HX, 16GB, 512GB, RTX 4060)',
    36990000,
    'Laptop gaming chuyên nghiệp với tản nhiệt Coldfront 5.0, chip Intel Core i7-13700HX, GPU RTX 4060 và màn hình 16 inch 240Hz sắc nét.',
    'ACTIVE',
    4.7,
    '{"screen": "16 inch WQXGA (2560x1600) IPS 240Hz 500 nits", "cpu": "Intel Core i7-13700HX (16 nhân, 24 luồng)", "gpu": "NVIDIA GeForce RTX 4060 8GB GDDR6", "ram": "16GB DDR5 4800MHz", "ssd": "512GB PCIe NVMe SSD"}',
    1850000000000001002,
    1850000000000002010,
    NOW(),
    NOW()
),
-- 14. Samsung Galaxy Tab S9 Ultra
(
    1850000000000003014,
    'Máy tính bảng Samsung Galaxy Tab S9 Ultra 256GB kèm S-Pen',
    25990000,
    'Tablet màn hình cực đại 14.6 inch Dynamic AMOLED 2X kèm bút S-Pen kháng nước chuẩn IP68, hỗ trợ chế độ Samsung DeX tối ưu hóa công việc.',
    'ACTIVE',
    4.8,
    '{"screen": "14.6 inch Dynamic AMOLED 2X 120Hz", "chip": "Snapdragon 8 Gen 2 for Galaxy", "ram": "12GB", "storage": "256GB", "battery": "11200 mAh, sạc nhanh 45W", "protection": "Kháng nước và bụi chuẩn IP68"}',
    1850000000000001003,
    1850000000000002002,
    NOW(),
    NOW()
),
-- 15. Marshall Stanmore III
(
    1850000000000003015,
    'Loa Bluetooth Marshall Stanmore III 80W',
    9490000,
    'Loa Bluetooth để bàn phong cách Vintage cổ điển với âm trường rộng hơn thế hệ trước, công suất 80W cùng công nghệ âm thanh cân bằng Dynamic Loudness.',
    'ACTIVE',
    4.9,
    '{"power": "80W Class D (1x 50W Woofer, 2x 15W Tweeter)", "frequency": "45Hz - 20,000Hz", "connectivity": "Bluetooth 5.2, AUX 3.5mm, Cổng RCA", "dimension": "350 x 195 x 185 mm", "weight": "4.25kg"}',
    1850000000000001004,
    1850000000000002009,
    NOW(),
    NOW()
),
-- 16. AirPods Pro 2 USB-C
(
    1850000000000003016,
    'Tai nghe True Wireless Apple AirPods Pro 2 (Hộp sạc MagSafe USB-C)',
    5790000,
    'Tai nghe in-ear khử tiếng ồn chủ động gấp 2 lần với chip H2, âm thanh thích ứng Adaptive Audio, sạc chuẩn USB-C và hộp sạc tích hợp loa tìm kiếm.',
    'ACTIVE',
    4.9,
    '{"chip": "Apple H2, chip U1 trong hộp sạc", "anc": "Chống ồn chủ động gấp 2 lần, Chế độ Xuyên âm thích ứng", "battery": "6 giờ (tai nghe), 30 giờ (kèm hộp sạc)", "charging": "Cổng USB-C, MagSafe, sạc không dây Qi", "waterResistance": "Kháng bụi, mồ hôi và nước chuẩn IP54"}',
    1850000000000001004,
    1850000000000002001,
    NOW(),
    NOW()
),
-- 17. Logitech MX Mechanical
(
    1850000000000003017,
    'Bàn phím cơ không dây Logitech MX Mechanical Fullsize',
    3490000,
    'Bàn phím cơ văn phòng switch low-profile hành trình ngắn êm ái, kết nối đa nền tảng tối đa 3 thiết bị và đèn nền thông minh tự phát sáng khi đưa tay tới gần.',
    'ACTIVE',
    4.8,
    '{"layout": "Fullsize (110 phím)", "switch": "Tactile Quiet (Low-profile)", "connectivity": "Bluetooth Low Energy, Logi Bolt USB Receiver", "battery": "Pin sạc Type-C dùng tới 10 tháng (tắt đèn nền)", "compatibility": "Windows, macOS, Linux, ChromeOS, iOS, Android"}',
    1850000000000001005,
    1850000000000002005,
    NOW(),
    NOW()
),
-- 18. Dell UltraSharp U2724D
(
    1850000000000003018,
    'Màn hình chuyên đồ họa Dell UltraSharp U2724D 27 inch 2K IPS Black 120Hz',
    10490000,
    'Màn hình chuyên đồ họa với tấm nền IPS Black độ tương phản 2000:1, tần số quét 120Hz mượt mà và cảm biến ánh sáng môi trường tự điều chỉnh độ sáng.',
    'ACTIVE',
    4.8,
    '{"size": "27 inch IPS Black (2560 x 1440 QHD)", "refreshRate": "120Hz", "contrastRatio": "2000:1", "colorAccuracy": "100% sRGB, 98% Display P3, Delta E < 2", "ports": "DisplayPort 1.4, HDMI 2.1, USB-C upstream/downstream, USB 3.2"}',
    1850000000000001006,
    1850000000000002003,
    NOW(),
    NOW()
),
-- 19. Samsung Galaxy Watch6 Classic 47mm LTE
(
    1850000000000003019,
    'Đồng hồ thông minh Samsung Galaxy Watch6 Classic 47mm LTE',
    7990000,
    'Smartwatch cổ điển sở hữu viền xoay bezel vật lý đặc trưng, mặt kính Sapphire bền bỉ, màn hình Super AMOLED sắc nét và tính năng đo chỉ số cơ thể BIA chuyên sâu.',
    'ACTIVE',
    4.7,
    '{"caseSize": "47mm thép không gỉ, viền xoay vật lý", "screen": "1.5 inch Super AMOLED 480x480, Sapphire Crystal", "chip": "Exynos W930 lõi kép 1.4GHz", "connectivity": "LTE (eSIM), Bluetooth 5.3, Wi-Fi, NFC, GPS", "health": "Cảm biến BIA 3 trong 1 (ECG, huyết áp, thành phần cơ thể)"}',
    1850000000000001007,
    1850000000000002002,
    NOW(),
    NOW()
),
-- 20. Xiaomi 14 5G
(
    1850000000000003020,
    'Điện thoại Xiaomi 14 5G 12GB/512GB (Ống kính Leica Summilux)',
    21990000,
    'Flagship nhỏ gọn 6.36 inch trang bị ống kính quang học Leica Summilux cao cấp, vi xử lý Snapdragon 8 Gen 3 và sạc siêu tốc 90W HyperCharge.',
    'ACTIVE',
    4.8,
    '{"screen": "6.36 inch LTPO OLED 120Hz 1.5K, độ sáng 3000 nits", "chip": "Snapdragon 8 Gen 3 (4nm)", "camera": "Bộ 3 camera Leica 50MP Summilux", "ram": "12GB LPDDR5X", "storage": "512GB UFS 4.0", "battery": "4610 mAh, sạc dây 90W, sạc không dây 50W"}',
    1850000000000001001,
    1850000000000002008,
    NOW(),
    NOW()
);

-- 4. Thêm Product Images cho từng Product (mỗi Product có 2 ảnh placeholder: 1 ảnh chính is_primary=1, 1 ảnh phụ is_primary=0)
INSERT INTO `product_images` (`id`, `image_url`, `display_order`, `is_primary`, `product_id`, `product_variant_id`) VALUES
-- Ảnh cho Product 1: iPhone 18 Pro Max
(1850000000000004001, 'http://localhost:8080/uploads/products/Apple-iPhone-18-Pro-2up-260909.jpg', 0, 1, 1850000000000003001, NULL),
(1850000000000004002, 'http://localhost:8080/uploads/products/Apple-iPhone-18-Pro-color-lineup-260909.jpg', 1, 0, 1850000000000003001, NULL),

-- Ảnh cho Product 2: Samsung Galaxy S24 Ultra
(1850000000000004003, 'https://www.trikart.com/media/catalog/product/g/a/galaxy_s24_ultra_gray-2_1.jpg?auto=webp&quality=90&width=2500', 0, 1, 1850000000000003002, NULL),
(1850000000000004004, 'https://bf1af2.akinoncloudcdn.com/products/2024/09/06/32338/c4274b1c-bd26-414d-812c-f9364346f2b1_size1920_cropCenter.jpg', 1, 0, 1850000000000003002, NULL),

-- Ảnh cho Product 3: MacBook Pro 14 M3
(1850000000000004005, 'https://media.studio7thailand.com/153581/TH_Macbook_Pro_M3_Space_Gray_PDP_Image_Position-1.png', 0, 1, 1850000000000003003, NULL),
(1850000000000004006, 'https://www.istudiobyspvi.com/cdn/shop/files/Macbook_Pro_M3_Space_Gray_PDP_Image_Position_6__GBEN_86a22807-b350-4fdc-81e4-081aaa60b4ea.jpg?v=1718122363&width=823', 1, 0, 1850000000000003003, NULL),
-- Ảnh cho Product 4: Dell XPS 15 9530
(1850000000000004007, 'https://bizweb.dktcdn.net/100/512/769/products/dell-xps-9530.jpg?v=1719650060813', 0, 1, 1850000000000003004, NULL),
(1850000000000004008, 'https://laptopmedia.com/wp-content/uploads/2023/03/4-3-scaled-e1677767797910.jpg', 1, 0, 1850000000000003004, NULL),

-- Ảnh cho Product 5: Sony WH-1000XM5
(1850000000000004009, 'https://i.ebayimg.com/images/g/fv0AAeSwumNpiqP6/s-l1200.jpg', 0, 1, 1850000000000003005, NULL),
(1850000000000004010, 'https://www.pbtech.com/pacific/imgprod/H/S/HSTSNY010050__5.jpg', 1, 0, 1850000000000003005, NULL),

-- Ảnh cho Product 6: iPad Pro 11 inch M4
(1850000000000004011, 'https://cdn11.bigcommerce.com/s-d9i7h6tt0z/images/stencil/original/products/1324/13372/03__56417.1723280951.jpg?c=1', 0, 1, 1850000000000003006, NULL),
(1850000000000004012, 'https://cdn11.bigcommerce.com/s-d9i7h6tt0z/images/stencil/1280x1280/products/1324/13371/02__99705.1763463719.jpg?c=1', 1, 0, 1850000000000003006, NULL),

-- Ảnh cho Product 7: Logitech MX Master 3S
(1850000000000004013, 'https://service.pcconnection.com/images/inhouse/670758B6-3C66-434A-8CB7-13DF67624E74.jpg', 0, 1, 1850000000000003007, NULL),
(1850000000000004014, 'https://product.hstatic.net/200000837185/product/mx-master-3s-mouse-top-side-view-pale-gray-copy_8b8e875720d94eff999dd90582edcf67_master.jpg', 1, 0, 1850000000000003007, NULL),

-- Ảnh cho Product 8: Asus ROG Azoth
(1850000000000004015, 'https://www.cybertek.fr/images_produits/9296671a-f732-45ef-ab09-899bf6fc9e42.png', 0, 1, 1850000000000003008, NULL),
(1850000000000004016, 'https://dlcdnwebimgs.asus.com/files/media/2C7F3DE4-F638-4ED3-8A25-83ABBBFF5F3F/v1/video/animation.jpg', 1, 0, 1850000000000003008, NULL),

-- Ảnh cho Product 9: LG UltraGear OLED 27GR95QE-B
(1850000000000004017, 'https://img.danawa.com/prod_img/500000/756/791/img/18791756_2.jpg?_v=20251111153840', 0, 1, 1850000000000003009, NULL),
(1850000000000004018, 'https://www.kamera-express.nl/media/f9abce2d-9585-43af-aade-4d3c666156b5/lg-ultragear-oled-27gr95qe-b.jpg', 1, 0, 1850000000000003009, NULL),

-- Ảnh cho Product 10: Apple Watch Series 9
(1850000000000004019, 'https://myistore.co.zm/cdn/shop/files/Apple_Watch_Series_9_GPS_45mm_Midnight_Aluminum_Midnight_Sport_Band_PDP_Image_Position-2__WWEN.jpg?v=1748335383', 0, 1, 1850000000000003010, NULL),
(1850000000000004020, 'https://cdn.mos.cms.futurecdn.net/4ZbRgR2FgBBHj4PMBRon88.jpg', 1, 0, 1850000000000003010, NULL),

-- Ảnh cho Product 11: Samsung Galaxy Z Fold5
(1850000000000004021, 'https://assets.products-live.ao.com/Images/01e30ac8-17e3-4dbd-97cf-feef1a7b4fdc/1280x1280/Galaxy_Fold5_Black_15a.jpg', 0, 1, 1850000000000003011, NULL),
(1850000000000004022, 'https://www.reuse.cl/cdn/shop/files/smartphone-samsung-galaxy-z-fold-5-5g-256gb-negro-reacondicionado-341404.png?v=1756247224&width=1214', 1, 0, 1850000000000003011, NULL),

-- Ảnh cho Product 12: Asus ROG Zephyrus G14
(1850000000000004023, 'https://cdn2.fptshop.com.vn/unsafe/rog_zephyrus_g14_2024_ga403_gray_1_994724b4c6.png', 0, 1, 1850000000000003012, NULL),
(1850000000000004024, 'https://cdn.ankhang.vn/media/product/30506_laptop_asus_rog_zephyrus_g14_ga403ww_qs136ws_4.jpg', 1, 0, 1850000000000003012, NULL),

-- Ảnh cho Product 13: Lenovo Legion Pro 5
(1850000000000004025, 'https://www.topmarket.co.il/images/detailed/250/255be4c73c54b9109da363980354318113c739d2.jpg', 0, 1, 1850000000000003013, NULL),
(1850000000000004026, 'https://www.topmarket.co.il/images/detailed/250/ca8f4f82426f27b3468d0da57f89d03d0969769c.png', 1, 0, 1850000000000003013, NULL),

-- Ảnh cho Product 14: Samsung Galaxy Tab S9 Ultra
(1850000000000004027, 'https://samsungbrshop.vtexassets.com/arquivos/ids/221617/Galaxy-Tab-S9-Ultra_Graphite_Product-Image_Front.jpg?v=639171488535700000', 0, 1, 1850000000000003014, NULL),
(1850000000000004028, 'https://www.telstra.com.au/content/dam/tcom/devices/mobile/mhdwtab-s9u/graphite/landscape-samsung-galaxyTabS9Ultra-graphite-03-900x1200.jpg', 1, 0, 1850000000000003014, NULL),

-- Ảnh cho Product 15: Marshall Stanmore III
(1850000000000004029, 'https://bcec.vn/upload/image/loa-marshall-stanmore-iii-black-1.webp', 0, 1, 1850000000000003015, NULL),
(1850000000000004030, 'https://bcec.vn/upload/image/loa-marshall-stanmore-iii-black-5.webp', 1, 0, 1850000000000003015, NULL),

-- Ảnh cho Product 16: AirPods Pro 2 USB-C
(1850000000000004031, 'https://img.pacifiko.com/PROD/resize/1/1000x1000/M2JhN2UxNT_242.jpg', 0, 1, 1850000000000003016, NULL),
(1850000000000004032, 'https://n.sinaimg.cn/spider20241031/480/w1440h1440/20241031/74f3-5c5b2a376fc7f0519bda34b85643d3b6.jpg', 1, 0, 1850000000000003016, NULL),

-- Ảnh cho Product 17: Logitech MX Mechanical
(1850000000000004033, 'https://www.officecrave.com/image/data/product_image_large/809642.JPG', 0, 1, 1850000000000003017, NULL),
(1850000000000004034, 'https://images1.kabum.com.br/produtos/fotos/356701/teclado-mecanico-sem-fio-logitech-mx-mechanical-iluminacao-switch-tactile-quiet-bluetooth-usb-preto-920-010760_1781629815_gg.jpg', 1, 0, 1850000000000003017, NULL),

-- Ảnh cho Product 18: Dell UltraSharp U2724D
(1850000000000004035, 'https://youget.pt/174484-large_default/dell-ultrasharp-u2724d-27-ips-qhd-120hz-monitor.jpg', 0, 1, 1850000000000003018, NULL),
(1850000000000004036, 'https://www.dellonline.ae/cdn/shop/files/B8590DF1-2E42-4DFE-A3A7-1A8794E36017_275771.jpg?v=1739187070&width=1500', 1, 0, 1850000000000003018, NULL),

-- Ảnh cho Product 19: Samsung Galaxy Watch6 Classic
(1850000000000004037, 'https://cdn.samsungshop.com.ua/products/8129/cover/159839/SM-R965_002_Front2_Silver.webp', 0, 1, 1850000000000003019, NULL),
(1850000000000004038, 'https://data.clickforshop.it/imgprodotto/samsung-galaxy-watch-6-classic-smartwatch-fitness-tracker-47-mm-gps-wi-fi-colore-argento-con-cinturino-argento_420830_zoom.jpg', 1, 0, 1850000000000003019, NULL),

-- Ảnh cho Product 20: Xiaomi 14 5G
(1850000000000004039, 'https://cdnpro.eraspace.com/media/catalog/product/x/i/xiaomi_14_black_2.jpg', 0, 1, 1850000000000003020, NULL),
(1850000000000004040, 'https://www.euronics.it/dw/image/v2/BFPN_PRD/on/demandware.static/-/Sites-catalog_euronics_master/default/dw8ff0c011/hi-res/242001768_4.jpg?q=90&strip=false&sw=1000', 1, 0, 1850000000000003020, NULL);

INSERT INTO `product_variants`
(`id`, `sku`, `price`, `attributes`, `image_url`, `status`, `product_id`)
VALUES

-- =====================================================
-- PRODUCT 1: iPhone 18 Pro Max
-- Official colors: Black, Silver, Burgundy, Glacier
-- =====================================================

(1850000000000005001, 'IP18PM-256-BLK', 29990000,
'{"Color": "Black", "Storage": "256GB"}',
'https://kontakt.az/media/catalog/product/cache/0a8af3db36f01f2d8dc4ea3efada0763/t/m/tm-dg-sbp-1105-sm-4380_1.png',
'ACTIVE', 1850000000000003001),

(1850000000000005002, 'IP18PM-256-SLV', 29990000,
'{"Color": "Silver", "Storage": "256GB"}',
'https://cdn.comfy.ua/media/catalog/product/cache/5/image/600x/9df78eab33525d08d6e5fb8d27136e95/4/e/4e6dca2d7961914c272ec74b960fc2b30a047641_bdc9f.jpg',
'ACTIVE', 1850000000000003001),

(1850000000000005003, 'IP18PM-512-BLK', 35990000,
'{"Color": "Black", "Storage": "512GB"}',
'https://kontakt.az/media/catalog/product/cache/0a8af3db36f01f2d8dc4ea3efada0763/t/m/tm-dg-sbp-1105-sm-4380_1.png',
'ACTIVE', 1850000000000003001),

(1850000000000005004, 'IP18PM-512-BRG', 35990000,
'{"Color": "Burgundy", "Storage": "512GB"}',
'https://bestshop.com.py/img/1000x1000/products/599107242/iphone-18-pro-max-256gb-a3473-ll-a-esim-burgundy-cover-c.jpg',
'ACTIVE', 1850000000000003001),

(1850000000000005005, 'IP18PM-1TB-GLC', 41990000,
'{"Color": "Glacier", "Storage": "1TB"}',
'https://i5.walmartimages.ca/asr/6d858c9d-0c90-4d27-8f52-9ef17c9419b8.97dbc09717692d6feb4b4ba6805b25bb.jpeg',
'ACTIVE', 1850000000000003001),


-- =====================================================
-- PRODUCT 2: Samsung Galaxy S24 Ultra
-- =====================================================

(1850000000000005006, 'S24U-256-BLK', 27990000,
'{"Color": "Titanium Black", "Storage": "256GB"}',
'https://need-a-phone.ca/cdn/shop/files/S24_Ultra_Black.png?v=1759743876&width=1946',
'ACTIVE', 1850000000000003002),

(1850000000000005007, 'S24U-256-GRY', 27990000,
'{"Color": "Titanium Gray", "Storage": "256GB"}',
'https://cdn.media.amplience.net/i/xcite/545738-01',
'ACTIVE', 1850000000000003002),

(1850000000000005008, 'S24U-256-VIO', 27990000,
'{"Color": "Titanium Violet", "Storage": "256GB"}',
'https://www.laptopsdirect.co.uk/Images/SM-S928BZVGEUB_1_Supersize.jpg?v=3',
'ACTIVE', 1850000000000003002),

(1850000000000005009, 'S24U-512-BLK', 31490000,
'{"Color": "Titanium Black", "Storage": "512GB"}',
'https://need-a-phone.ca/cdn/shop/files/S24_Ultra_Black.png?v=1759743876&width=1946',
'ACTIVE', 1850000000000003002),

(1850000000000005010, 'S24U-1TB-YLW', 36990000,
'{"Color": "Titanium Yellow", "Storage": "1TB"}',
'https://cdn.mos.cms.futurecdn.net/kXyHQ9acTgrH33sdgtcQBR.jpg',
'ACTIVE', 1850000000000003002),


-- =====================================================
-- PRODUCT 3: MacBook Pro 14 M3 Pro / M3 Max
-- =====================================================

(1850000000000005011, 'MBP14M3-18-512-SBK', 39990000,
'{"Color": "Space Black", "RAM": "18GB", "Storage": "512GB"}',
'https://macpro-la.com/cdn/shop/files/Apple_MacBook_Pro_14_inchi_1024x1024.png?v=1781034619',
'ACTIVE', 1850000000000003003),

(1850000000000005012, 'MBP14M3-18-512-SV', 39990000,
'{"Color": "Silver", "RAM": "18GB", "Storage": "512GB"}',
'https://www.istudio.store/cdn/shop/files/TH_Macbook_Pro_M3_Silver_PDP_Image_Position-1_cccfd8b6-d64c-4d9c-976e-77f55f76a189.jpg?v=1707290692',
'ACTIVE', 1850000000000003003),

(1850000000000005013, 'MBP14M3-18-1TB-SBK', 46990000,
'{"Color": "Space Black", "RAM": "18GB", "Storage": "1TB"}',
'https://macpro-la.com/cdn/shop/files/Apple_MacBook_Pro_14_inchi_1024x1024.png?v=1781034619',
'ACTIVE', 1850000000000003003),

(1850000000000005014, 'MBP14M3-36-1TB-SV', 58990000,
'{"Color": "Silver", "RAM": "36GB", "Storage": "1TB"}',
'https://www.istudio.store/cdn/shop/files/TH_Macbook_Pro_M3_Silver_PDP_Image_Position-1_cccfd8b6-d64c-4d9c-976e-77f55f76a189.jpg?v=1707290692',
'ACTIVE', 1850000000000003003),


-- =====================================================
-- PRODUCT 4: Dell XPS 15 9530
-- Official finish: Platinum Silver
-- =====================================================

(1850000000000005015, 'XPS15-16-512-PLT', 42500000,
'{"Color": "Platinum", "RAM": "16GB", "Storage": "512GB"}',
'https://bizweb.dktcdn.net/100/512/769/products/dell-xps-9530.jpg?v=1719650060813',
'ACTIVE', 1850000000000003004),

(1850000000000005016, 'XPS15-32-1TB-PLT', 51500000,
'{"Color": "Platinum", "RAM": "32GB", "Storage": "1TB"}',
'https://bizweb.dktcdn.net/100/512/769/products/dell-xps-9530.jpg?v=1719650060813',
'ACTIVE', 1850000000000003004),

(1850000000000005017, 'XPS15-64-1TB-PLT', 52000000,
'{"Color": "Platinum", "RAM": "64GB", "Storage": "1TB"}',
'https://bizweb.dktcdn.net/100/512/769/products/dell-xps-9530.jpg?v=1719650060813',
'ACTIVE', 1850000000000003004),


-- =====================================================
-- PRODUCT 5: Sony WH-1000XM5
-- =====================================================

(1850000000000005018, 'WH1000XM5-BLK', 7990000,
'{"Color": "Black"}',
'https://www.sony.com/is/image/gwtprod/6145c1d32e6ac8e63a46c912dc33c5bb?fmt=png-alpha&hei=515&trf=trim&wid=515',
'ACTIVE', 1850000000000003005),

(1850000000000005019, 'WH1000XM5-SLV', 7990000,
'{"Color": "Silver"}',
'https://www.sony.com/is/image/gwtprod/07c9dca1a0e976a4760c609a8ef7d3ac?fmt=png-alpha&hei=515&trf=trim&wid=515',
'ACTIVE', 1850000000000003005),

(1850000000000005020, 'WH1000XM5-BLU', 8190000,
'{"Color": "Midnight Blue"}',
'https://cdn.etoren.com/upload/images/0.22871500_1689042093_sony-wh-1000x-m5-wireless-nc-headphone-midnight-blue.jpg',
'ACTIVE', 1850000000000003005),


-- =====================================================
-- PRODUCT 6: iPad Pro 11 M4
-- =====================================================

(1850000000000005021, 'IPADPRO11-256-WF-SLV', 28990000,
'{"Color": "Silver", "Storage": "256GB", "Connectivity": "Wi-Fi"}',
'https://www.alasil.ae/cdn/shop/files/iPad_Pro-Silver-1.webp?v=1766363435&width=1946',
'ACTIVE', 1850000000000003006),

(1850000000000005022, 'IPADPRO11-256-WF-BLK', 28990000,
'{"Color": "Space Black", "Storage": "256GB", "Connectivity": "Wi-Fi"}',
'https://cdn.media.amplience.net/i/xcite/657232-02',
'ACTIVE', 1850000000000003006),

(1850000000000005023, 'IPADPRO11-512-WF-BLK', 34990000,
'{"Color": "Space Black", "Storage": "512GB", "Connectivity": "Wi-Fi"}',
'https://cdn.media.amplience.net/i/xcite/657232-02',
'ACTIVE', 1850000000000003006),

(1850000000000005024, 'IPADPRO11-256-5G-SLV', 33990000,
'{"Color": "Silver", "Storage": "256GB", "Connectivity": "Wi-Fi + 5G"}',
'https://www.alasil.ae/cdn/shop/files/iPad_Pro-Silver-1.webp?v=1766363435&width=1946',
'ACTIVE', 1850000000000003006),

(1850000000000005025, 'IPADPRO11-1TB-5G-BLK', 52990000,
'{"Color": "Space Black", "Storage": "1TB", "Connectivity": "Wi-Fi + 5G"}',
'https://cdn.media.amplience.net/i/xcite/657232-02',
'ACTIVE', 1850000000000003006),


-- =====================================================
-- PRODUCT 7: Logitech MX Master 3S
-- =====================================================

(1850000000000005026, 'MXM3S-GPH', 2190000,
'{"Color": "Graphite"}',
'https://cdn.cs.1worldsync.com/67/07/670758b6-3c66-434a-8cb7-13df67624e74.jpg',
'ACTIVE', 1850000000000003007),

(1850000000000005027, 'MXM3S-PGY', 2190000,
'{"Color": "Pale Gray"}',
'https://product.hstatic.net/200000837185/product/mx-master-3s-mouse-top-side-view-pale-gray-copy_8b8e875720d94eff999dd90582edcf67_master.jpg',
'ACTIVE', 1850000000000003007),

(1850000000000005028, 'MXM3S-MAC-SG', 2390000,
'{"Color": "Space Gray", "Edition": "For Mac"}',
'https://www.logitech.com/content/dam/logitech/en/products/mice/mx-master-3s-mac-bluetooth-mouse/gallery/space-grey/mx-master-3s-for-mac-mouse-front-view-space-grey.png',
'ACTIVE', 1850000000000003007),


-- =====================================================
-- PRODUCT 8: Asus ROG Azoth 75%
-- =====================================================

(1850000000000005029, 'AZOTH-BLK-NXRED', 5990000,
'{"Color": "Black", "Switch": "NX Red"}',
'https://www.proshop.se/Images/1600x1200/3135222_c129b5b3ea60.png',
'ACTIVE', 1850000000000003008),

(1850000000000005030, 'AZOTH-BLK-NXBRN', 5990000,
'{"Color": "Black", "Switch": "NX Brown"}',
'https://www.proshop.se/Images/1600x1200/3135222_c129b5b3ea60.png',
'ACTIVE', 1850000000000003008),

(1850000000000005031, 'AZOTH-BLK-NXBLU', 5990000,
'{"Color": "Black", "Switch": "NX Blue"}',
'https://www.proshop.se/Images/1600x1200/3135222_c129b5b3ea60.png',
'ACTIVE', 1850000000000003008),

(1850000000000005032, 'AZOTH-WHT-NXRED', 6290000,
'{"Color": "Moonlight White", "Switch": "NX Red"}',
'https://dlcdnwebimgs.asus.com/gain/B35A9B08-F6BA-4B7C-9383-5D04444D2A1F/w1000/h732',
'ACTIVE', 1850000000000003008),


-- =====================================================
-- PRODUCT 9: LG UltraGear OLED 27GR95QE-B
-- =====================================================

(1850000000000005033, 'LG27GR95-STD', 18900000,
'{"Stand": "Standard Stand", "Warranty": "24 months"}',
'https://www.lg.com/content/dam/channel/wcms/vn/images/it-monitors/27gr95qe-b_atv_eavh_vn_c/450.jpg',
'ACTIVE', 1850000000000003009),

(1850000000000005034, 'LG27GR95-ARM', 20100000,
'{"Stand": "Monitor Arm Bundle", "Warranty": "24 months"}',
'https://www.lg.com/content/dam/channel/wcms/vn/images/it-monitors/27gr95qe-b_atv_eavh_vn_c/450.jpg',
'ACTIVE', 1850000000000003009),

(1850000000000005035, 'LG27GR95-W36', 20400000,
'{"Stand": "Standard Stand", "Warranty": "36 months"}',
'https://www.lg.com/content/dam/channel/wcms/vn/images/it-monitors/27gr95qe-b_atv_eavh_vn_c/450.jpg',
'ACTIVE', 1850000000000003009),


-- =====================================================
-- PRODUCT 10: Apple Watch Series 9 GPS 45mm
-- =====================================================

(1850000000000005036, 'AWS9-45-MID', 10490000,
'{"Color": "Midnight", "Size": "45mm"}',
'https://cdn.mafrservices.com/pim-content/SAU/media/product/741820/1730026204/741820_main.jpg',
'ACTIVE', 1850000000000003010),

(1850000000000005037, 'AWS9-45-STL', 10490000,
'{"Color": "Starlight", "Size": "45mm"}',
'https://electroworld.abenson.com/media/catalog/product/1/8/189794_2023.jpg',
'ACTIVE', 1850000000000003010),

(1850000000000005038, 'AWS9-45-SLV', 10490000,
'{"Color": "Silver", "Size": "45mm"}',
'https://cdn.media.amplience.net/i/xcite/654314-02?fmt=auto&img404=default&qlt=75&w=2048',
'ACTIVE', 1850000000000003010),

(1850000000000005039, 'AWS9-45-RED', 10690000,
'{"Color": "(PRODUCT)RED", "Size": "45mm"}',
'https://pcbox.vtexassets.com/arquivos/ids/2304752/MRYG3QL-A_GAL_2.jpg?v=638600029548570000',
'ACTIVE', 1850000000000003010),


-- =====================================================
-- PRODUCT 11: Samsung Galaxy Z Fold5
-- =====================================================

(1850000000000005040, 'ZFOLD5-256-BLK', 31990000,
'{"Color": "Phantom Black", "Storage": "256GB"}',
'https://digitalwalker.ph/cdn/shop/files/SAMUNTGALAXYZFOLD5PHANTOMBLACK1_1000x1000.png?v=1692251947',
'ACTIVE', 1850000000000003011),

(1850000000000005041, 'ZFOLD5-512-BLK', 34990000,
'{"Color": "Phantom Black", "Storage": "512GB"}',
'https://digitalwalker.ph/cdn/shop/files/SAMUNTGALAXYZFOLD5PHANTOMBLACK1_1000x1000.png?v=1692251947',
'ACTIVE', 1850000000000003011),

(1850000000000005042, 'ZFOLD5-512-BLU', 34990000,
'{"Color": "Icy Blue", "Storage": "512GB"}',
'https://multimedia.bbycastatic.ca/multimedia/products/500x500/177/17733/17733158.jpeg',
'ACTIVE', 1850000000000003011),

(1850000000000005043, 'ZFOLD5-512-CRM', 34990000,
'{"Color": "Cream", "Storage": "512GB"}',
'https://www.virginmegastore.ae/medias/83466-main.jpg',
'ACTIVE', 1850000000000003011),

(1850000000000005044, 'ZFOLD5-1TB-BLK', 40990000,
'{"Color": "Phantom Black", "Storage": "1TB"}',
'https://digitalwalker.ph/cdn/shop/files/SAMUNTGALAXYZFOLD5PHANTOMBLACK1_1000x1000.png?v=1692251947',
'ACTIVE', 1850000000000003011),


-- =====================================================
-- PRODUCT 12: Asus ROG Zephyrus G14
-- =====================================================

(1850000000000005045, 'G14-16-1TB-GRY', 49990000,
'{"Color": "Eclipse Gray", "RAM": "16GB", "Storage": "1TB"}',
'https://mediazone.ma/product/images/15457-HZhXT68h/asus-rog-zephyrus-g14-ga403-2024-amd-ryzen-7-8845hs-16go-1to-ssd-rtx-4060-14-oled-3k-120hz-eclipse-gray-maroc.webp',
'ACTIVE', 1850000000000003012),

(1850000000000005046, 'G14-16-1TB-WHT', 50490000,
'{"Color": "Platinum White", "RAM": "16GB", "Storage": "1TB"}',
'https://pisces.bbystatic.com/image2/BestBuy_US/images/products/ff445e01-b15d-4e4a-b92c-89a1975e4408.jpg%3BmaxHeight%3D1920%3BmaxWidth%3D900?format=webp',
'ACTIVE', 1850000000000003012),

(1850000000000005047, 'G14-32-1TB-GRY', 54990000,
'{"Color": "Eclipse Gray", "RAM": "32GB", "Storage": "1TB"}',
'https://mediazone.ma/product/images/15457-HZhXT68h/asus-rog-zephyrus-g14-ga403-2024-amd-ryzen-7-8845hs-16go-1to-ssd-rtx-4060-14-oled-3k-120hz-eclipse-gray-maroc.webp',
'ACTIVE', 1850000000000003012),


-- =====================================================
-- PRODUCT 13: Lenovo Legion Pro 5
-- =====================================================

(1850000000000005048, 'LEGION5-16-512-4060', 36990000,
'{"RAM": "16GB", "Storage": "512GB", "GPU": "RTX 4060"}',
'https://ttcenter.com.vn/uploads/product/9jdaoh23-965-lenovo-legion-pro-5-2023-core-i7-13700hx-16gb-1tb-rtx-4060-8gb-16-2k-165hz-new.png',
'ACTIVE', 1850000000000003013),

(1850000000000005049, 'LEGION5-32-1TB-4060', 43990000,
'{"RAM": "32GB", "Storage": "1TB", "GPU": "RTX 4060"}',
'https://ttcenter.com.vn/uploads/product/9jdaoh23-965-lenovo-legion-pro-5-2023-core-i7-13700hx-16gb-1tb-rtx-4060-8gb-16-2k-165hz-new.png',
'ACTIVE', 1850000000000003013),

(1850000000000005050, 'LEGION5-32-1TB-4070', 51990000,
'{"RAM": "32GB", "Storage": "1TB", "GPU": "RTX 4070"}',
'https://prod-api.mediaexpert.pl/api/images/gallery/thumbnails/images/67/6786760/Laptop-LENOVO-Legion-Pro-5-16IRX9-frontowe.jpg',
'ACTIVE', 1850000000000003013),


-- =====================================================
-- PRODUCT 14: Samsung Galaxy Tab S9 Ultra
-- =====================================================

(1850000000000005051, 'TABS9U-256-GRA-WF', 25990000,
'{"Color": "Graphite", "Storage": "256GB", "Connectivity": "Wi-Fi"}',
'https://images.samsung.com/kdp/goods/2023/07/06/63e7801f-b75a-45a8-bdbc-16a3a8cf3d4e.png',
'ACTIVE', 1850000000000003014),

(1850000000000005052, 'TABS9U-256-BGE-WF', 25990000,
'{"Color": "Beige", "Storage": "256GB", "Connectivity": "Wi-Fi"}',
'https://images.samsung.com/kdp/goods/2023/07/06/a19bd334-1c34-40be-a679-aba35dda8f3c.png',
'ACTIVE', 1850000000000003014),

(1850000000000005053, 'TABS9U-512-GRA-WF', 29990000,
'{"Color": "Graphite", "Storage": "512GB", "Connectivity": "Wi-Fi"}',
'https://images.samsung.com/kdp/goods/2023/07/06/63e7801f-b75a-45a8-bdbc-16a3a8cf3d4e.png',
'ACTIVE', 1850000000000003014),

(1850000000000005054, 'TABS9U-1TB-GRA-5G', 37990000,
'{"Color": "Graphite", "Storage": "1TB", "Connectivity": "Wi-Fi + 5G"}',
'https://images.samsung.com/kdp/goods/2023/07/06/63e7801f-b75a-45a8-bdbc-16a3a8cf3d4e.png',
'ACTIVE', 1850000000000003014),


-- =====================================================
-- PRODUCT 15: Marshall Stanmore III
-- =====================================================

(1850000000000005055, 'STANMORE3-BLK', 9490000,
'{"Color": "Black"}',
'https://www.jbhifi.com.au/cdn/shop/products/640422-Product-0-I-638178969614459607.jpg?v=1682300236',
'ACTIVE', 1850000000000003015),

(1850000000000005056, 'STANMORE3-CRM', 9490000,
'{"Color": "Cream"}',
'https://bf1af2.a-cdn.akinoncloud.com/products/2024/09/06/42903/9c6ac5e2-0344-498c-8169-49ed1d654d4e_size3840_cropCenter.jpg',
'ACTIVE', 1850000000000003015),

(1850000000000005057, 'STANMORE3-BRN', 9790000,
'{"Color": "Brown"}',
'https://img-zuhalmuzik.mncdn.com/mnresize/1000/1000/images/product/163701ZD.1006080_3.jpg',
'ACTIVE', 1850000000000003015),


-- =====================================================
-- PRODUCT 16: AirPods Pro 2 USB-C
-- =====================================================

(1850000000000005058, 'APP2-STD', 5790000,
'{"Package": "Standard"}',
'https://oneclick.com.py/upload/produtos/aef03566c596b2e47cb94c660f6b0c6e.jpg',
'ACTIVE', 1850000000000003016),

(1850000000000005059, 'APP2-CASE', 6090000,
'{"Package": "Kèm ốp silicon"}',
'https://oneclick.com.py/upload/produtos/aef03566c596b2e47cb94c660f6b0c6e.jpg',
'ACTIVE', 1850000000000003016),

(1850000000000005060, 'APP2-CARE', 7690000,
'{"Package": "Kèm AppleCare+"}',
'https://oneclick.com.py/upload/produtos/aef03566c596b2e47cb94c660f6b0c6e.jpg',
'ACTIVE', 1850000000000003016),


-- =====================================================
-- PRODUCT 17: Logitech MX Mechanical
-- =====================================================

(1850000000000005061, 'MXMECH-FULL-TQ', 3490000,
'{"Layout": "Fullsize", "Switch": "Tactile Quiet"}',
'https://product.hstatic.net/200000722513/product/hanical-keyboard-top-view-graphite-us_602a1fb095904851880b1da99a10fb6a_c609e86185f249e78554c198895da48f_master.png',
'ACTIVE', 1850000000000003017),

(1850000000000005062, 'MXMECH-FULL-LIN', 3490000,
'{"Layout": "Fullsize", "Switch": "Linear"}',
'https://product.hstatic.net/200000722513/product/hanical-keyboard-top-view-graphite-us_602a1fb095904851880b1da99a10fb6a_c609e86185f249e78554c198895da48f_master.png',
'ACTIVE', 1850000000000003017),

(1850000000000005063, 'MXMECH-FULL-CLK', 3490000,
'{"Layout": "Fullsize", "Switch": "Clicky"}',
'https://product.hstatic.net/200000722513/product/hanical-keyboard-top-view-graphite-us_602a1fb095904851880b1da99a10fb6a_c609e86185f249e78554c198895da48f_master.png',
'ACTIVE', 1850000000000003017),

(1850000000000005064, 'MXMECH-MINI-TQ', 3290000,
'{"Layout": "Mini", "Switch": "Tactile Quiet"}',
'https://cdn.cclonline.com/cdn-cgi/image/width%3D2000/images/shopblocks/mx-mechanical-mini-mini-keyboard-top-view-graphite-uk.png',
'ACTIVE', 1850000000000003017),


-- =====================================================
-- PRODUCT 18: Dell UltraSharp U2724D
-- =====================================================

(1850000000000005065, 'U2724D-STD', 10490000,
'{"Stand": "Standard Stand", "Warranty": "36 months"}',
'https://cdn.cs.1worldsync.com/83/5a/835a6ac1-6bb2-4cab-b4ed-4bb4df00d908.jpg',
'ACTIVE', 1850000000000003018),

(1850000000000005066, 'U2724D-ARM', 11690000,
'{"Stand": "Monitor Arm Bundle", "Warranty": "36 months"}',
'https://cdn.cs.1worldsync.com/83/5a/835a6ac1-6bb2-4cab-b4ed-4bb4df00d908.jpg',
'ACTIVE', 1850000000000003018),

(1850000000000005067, 'U2724D-PREM', 11290000,
'{"Stand": "Standard Stand", "Warranty": "Premium Panel Exchange"}',
'https://cdn.cs.1worldsync.com/83/5a/835a6ac1-6bb2-4cab-b4ed-4bb4df00d908.jpg',
'ACTIVE', 1850000000000003018),


-- =====================================================
-- PRODUCT 19: Samsung Galaxy Watch6 Classic LTE
-- =====================================================

(1850000000000005068, 'GW6C-47-BLK', 7990000,
'{"Color": "Black", "Size": "47mm"}',
'https://image-us.samsung.com/SamsungUS/home/mobile/wearables/r965uzsblack/R965UZSblack1.jpg',
'ACTIVE', 1850000000000003019),

(1850000000000005069, 'GW6C-47-SLV', 7990000,
'{"Color": "Silver", "Size": "47mm"}',
'https://multimedia.bbycastatic.ca/multimedia/products/1500x1500/171/17187/17187090.jpg',
'ACTIVE', 1850000000000003019),

(1850000000000005070, 'GW6C-43-BLK', 7490000,
'{"Color": "Black", "Size": "43mm"}',
'https://www.mobileciti.com.au/media/catalog/product/s/a/samsung-galaxy-watch6-classic-43mm-black-5_1.jpg?image-type=image&store=mobileciti',
'ACTIVE', 1850000000000003019),

(1850000000000005071, 'GW6C-43-SLV', 7490000,
'{"Color": "Silver", "Size": "43mm"}',
'https://www.jbhifi.com.au/cdn/shop/products/654754-Product-0-I-638307594072462834.jpg?v=1695162683',
'ACTIVE', 1850000000000003019),


-- =====================================================
-- PRODUCT 20: Xiaomi 14 5G
-- =====================================================

(1850000000000005072, 'XM14-12-256-BLK', 19990000,
'{"Color": "Black", "RAM": "12GB", "Storage": "256GB"}',
'https://lcdn.altex.ro/media/catalog/product/t/e/telefon_xiaomi_14_black_674b656b.jpg',
'ACTIVE', 1850000000000003020),

(1850000000000005073, 'XM14-12-512-BLK', 21990000,
'{"Color": "Black", "RAM": "12GB", "Storage": "512GB"}',
'https://lcdn.altex.ro/media/catalog/product/t/e/telefon_xiaomi_14_black_674b656b.jpg',
'ACTIVE', 1850000000000003020),

(1850000000000005074, 'XM14-12-512-WHT', 21990000,
'{"Color": "White", "RAM": "12GB", "Storage": "512GB"}',
'https://www.powerplanetonline.com/cdnassets/xiaomi_14_blanco_01_l.jpg',
'ACTIVE', 1850000000000003020),

(1850000000000005075, 'XM14-12-512-GRN', 22290000,
'{"Color": "Jade Green", "RAM": "12GB", "Storage": "512GB"}',
'https://m.media-amazon.com/images/I/51tzoPJpLbL._AC_US1000_.jpg',
'ACTIVE', 1850000000000003020);2-512-GRN', 22290000, '{"Color": "Jade Green", "RAM": "12GB", "Storage": "512GB"}', 'https://cdnpro.eraspace.com/media/catalog/product/x/i/xiaomi_14_black_2.jpg', 'ACTIVE', 1850000000000003020);

-- 6. Thêm Inventory (tồn kho) cho mỗi variant vừa tạo (quantity ngẫu nhiên 10 - 100)
-- Mỗi variant có đúng 1 inventory (product_variant_id là unique).
INSERT INTO `inventories` (`id`, `quantity`, `updated_at`, `product_variant_id`)
SELECT `id` + 1000, FLOOR(10 + RAND() * 91), NOW(), `id`
FROM `product_variants`
WHERE `id` BETWEEN 1850000000000005001 AND 1850000000000005075;