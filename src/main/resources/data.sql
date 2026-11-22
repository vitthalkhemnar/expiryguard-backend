-- ============================================================
-- ExpiryGuard Production-Grade PostgreSQL Seed Data
-- Currency: Indian Rupee (INR / ₹)
-- All dates use dynamic PostgreSQL date arithmetic (CURRENT_DATE + N / - N)
-- Default Credentials:
--   admin / admin123 (ROLE_ADMIN)
--   cashier / cashier123 (ROLE_CASHIER)
-- ============================================================

-- 1. Initial System Users (Pre-hashed with BCrypt)
INSERT INTO users (username, password, full_name, role, is_active, created_at)
VALUES 
('admin', '$2a$10$aoLuea5ZpkpyuVYvWJe./uE7pLAkkbTeBkZSdV1yr9kkneRMEGT4q', 'Store Operations Manager', 'ROLE_ADMIN', true, CURRENT_TIMESTAMP),
('cashier', '$2a$10$dcIQCSnsyuRxsNKop2kBlOUV2SG9rSfaElGzf6SGPvwYytAJvnV62', 'Chief Cashier Counter 1', 'ROLE_CASHIER', true, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- 2. Product Categories
INSERT INTO categories (name, description, created_at)
VALUES 
('Pharmaceuticals', 'Prescription & Over-the-counter medicines, antibiotics, and analgesics', CURRENT_TIMESTAMP),
('Dairy & Packaged Foods', 'Fast-moving perishables, milk, butter, and processed foods', CURRENT_TIMESTAMP),
('Personal Care & Hygiene', 'Antiseptics, soaps, sanitisers, and hygiene products', CURRENT_TIMESTAMP),
('Groceries & Staples', 'Household food staples, grains, spices, and cooking ingredients', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- 3. Core Products
INSERT INTO products (sku, barcode, name, description, category_id, min_reorder_level, is_active, created_at)
VALUES
('MED-DOLO-650', '8901117001018', 'Dolo 650mg Paracetamol Tablets (15 Tabs)', 'Paracetamol 650mg antipyretic & analgesic strip', 1, 30, true, CURRENT_TIMESTAMP),
('MED-AUG-625', '8901117002025', 'Augmentin 625 Duo Tablets (10 Tabs)', 'Amoxicillin and Potassium Clavulanate antibacterial strip', 1, 15, true, CURRENT_TIMESTAMP),
('DRY-AMUL-1L', '8901262010052', 'Amul Taaza Homogenised Toned Milk 1L', 'UHT treated long life toned milk carton', 2, 25, true, CURRENT_TIMESTAMP),
('DRY-BTR-500', '8901262020082', 'Amul Butter Pasteurised 500g', 'Classic Indian salted dairy butter brick', 2, 10, true, CURRENT_TIMESTAMP),
('GRO-TATA-1KG', '8901030010014', 'Tata Salt Vacuum Evaporated Iodised 1kg', 'Pure vacuum evaporated iodised table salt pack', 4, 40, true, CURRENT_TIMESTAMP),
('PC-DETT-250', '8901396001018', 'Dettol Antiseptic Disinfectant Liquid 250ml', 'Chloroxylenol antibacterial first aid and hygiene liquid', 3, 20, true, CURRENT_TIMESTAMP)
ON CONFLICT (sku) DO NOTHING;

-- 4. Dynamic Markdown Tier Rules
INSERT INTO markdown_rules (rule_name, days_before_expiry, discount_percentage, is_active, created_at)
VALUES
('Critical Clearance (≤ 7 Days)', 7, 50, true, CURRENT_TIMESTAMP),
('Near Expiry Markdown (≤ 30 Days)', 30, 20, true, CURRENT_TIMESTAMP),
('Early Clearance (≤ 60 Days)', 60, 10, true, CURRENT_TIMESTAMP)
ON CONFLICT (days_before_expiry) DO NOTHING;

-- 5. Inventory Batches (Dynamic Expiry, Pricing in INR ₹)
-- Dolo 650 Batches:
--   Batch 1: Expiring in 4 days (Critical Clearance: 50% discount -> ₹17.00)
--   Batch 2: Expiring in 18 days (Near Expiry: 20% discount -> ₹27.20)
--   Batch 3: Expiring in 180 days (Fresh Stock: 0% discount -> ₹34.00)
INSERT INTO batches (product_id, batch_number, initial_quantity, available_quantity, cost_price, base_selling_price, current_selling_price, discount_percentage, manufacturing_date, expiry_date, status, version, created_at, updated_at)
VALUES
(1, 'DOLO-2024-C04', 30, 25, 20.00, 34.00, 17.00, 50, CURRENT_DATE - 120, CURRENT_DATE + 4, 'CRITICAL', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'DOLO-2024-N18', 50, 45, 20.00, 34.00, 27.20, 20, CURRENT_DATE - 70, CURRENT_DATE + 18, 'NEAR_EXPIRY', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 'DOLO-2024-F180', 200, 200, 20.00, 34.00, 34.00, 0, CURRENT_DATE - 10, CURRENT_DATE + 180, 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (batch_number) DO NOTHING;

-- Augmentin 625 Duo Batches:
--   Batch 1: Expiring in 5 days (Critical Clearance: 50% discount -> ₹102.50)
--   Batch 2: Expiring in 210 days (Fresh Stock: 0% discount -> ₹205.00)
INSERT INTO batches (product_id, batch_number, initial_quantity, available_quantity, cost_price, base_selling_price, current_selling_price, discount_percentage, manufacturing_date, expiry_date, status, version, created_at, updated_at)
VALUES
(2, 'AUG-2024-C05', 20, 15, 140.00, 205.00, 102.50, 50, CURRENT_DATE - 150, CURRENT_DATE + 5, 'CRITICAL', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'AUG-2024-F210', 100, 100, 140.00, 205.00, 205.00, 0, CURRENT_DATE - 15, CURRENT_DATE + 210, 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (batch_number) DO NOTHING;

-- Amul Milk Batches:
--   Batch 1: Expiring in 3 days (Critical Clearance: 50% discount -> ₹34.00)
--   Batch 2: Expiring in 80 days (Fresh Stock: 0% discount -> ₹68.00)
INSERT INTO batches (product_id, batch_number, initial_quantity, available_quantity, cost_price, base_selling_price, current_selling_price, discount_percentage, manufacturing_date, expiry_date, status, version, created_at, updated_at)
VALUES
(3, 'MILK-2024-C03', 30, 20, 52.00, 68.00, 34.00, 50, CURRENT_DATE - 87, CURRENT_DATE + 3, 'CRITICAL', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'MILK-2024-F80', 80, 80, 52.00, 68.00, 68.00, 0, CURRENT_DATE - 10, CURRENT_DATE + 80, 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (batch_number) DO NOTHING;

-- Amul Butter Batches:
--   Batch 1: Expiring in 22 days (Near Expiry: 20% discount -> ₹220.00)
--   Batch 2: Expiring in 150 days (Fresh Stock: 0% discount -> ₹275.00)
INSERT INTO batches (product_id, batch_number, initial_quantity, available_quantity, cost_price, base_selling_price, current_selling_price, discount_percentage, manufacturing_date, expiry_date, status, version, created_at, updated_at)
VALUES
(4, 'BTR-2024-N22', 25, 20, 210.00, 275.00, 220.00, 20, CURRENT_DATE - 130, CURRENT_DATE + 22, 'NEAR_EXPIRY', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'BTR-2024-F150', 60, 60, 210.00, 275.00, 275.00, 0, CURRENT_DATE - 20, CURRENT_DATE + 150, 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (batch_number) DO NOTHING;

-- Tata Salt Batch:
--   Batch 1: Expiring in 365 days (Fresh Stock: 0% discount -> ₹28.00)
INSERT INTO batches (product_id, batch_number, initial_quantity, available_quantity, cost_price, base_selling_price, current_selling_price, discount_percentage, manufacturing_date, expiry_date, status, version, created_at, updated_at)
VALUES
(5, 'SALT-2024-F365', 120, 120, 20.00, 28.00, 28.00, 0, CURRENT_DATE - 30, CURRENT_DATE + 365, 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (batch_number) DO NOTHING;

-- Dettol Antiseptic Batches:
--   Batch 1: Expiring in 28 days (Near Expiry: 20% discount -> ₹140.00)
--   Batch 2: Expiring in 330 days (Fresh Stock: 0% discount -> ₹175.00)
INSERT INTO batches (product_id, batch_number, initial_quantity, available_quantity, cost_price, base_selling_price, current_selling_price, discount_percentage, manufacturing_date, expiry_date, status, version, created_at, updated_at)
VALUES
(6, 'DETT-2024-N28', 20, 16, 130.00, 175.00, 140.00, 20, CURRENT_DATE - 330, CURRENT_DATE + 28, 'NEAR_EXPIRY', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'DETT-2024-F330', 90, 90, 130.00, 175.00, 175.00, 0, CURRENT_DATE - 35, CURRENT_DATE + 330, 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (batch_number) DO NOTHING;
