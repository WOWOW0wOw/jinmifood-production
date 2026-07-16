UPDATE categories SET name = '짝태', display_order = 1 WHERE slug = 'jjagtae';
UPDATE categories SET name = '먹태', display_order = 2 WHERE slug = 'meoktae';
UPDATE categories SET name = '장아찌', display_order = 3 WHERE slug = 'jangajji';
UPDATE categories SET name = '미역', display_order = 4 WHERE slug = 'miyeok';
UPDATE categories SET name = '오징어', display_order = 5 WHERE slug = 'squid';
UPDATE categories SET name = '가공식품', display_order = 6 WHERE slug = 'processed';

INSERT INTO products
    (name, slug, price, original_price, stock, summary, description, image_url,
     origin, manufacturer, weight, shelf_life, storage_method, active, featured,
     category_id, created_at, updated_at)
SELECT name, slug, price, original_price, stock, summary, description,
       '/images/product-placeholder.svg', '상품 포장지 별도 표기', '상품 포장지 별도 표기',
       '상품명 및 상세정보 참조', '상품 포장지 별도 표기', '서늘한 곳, 냉장 또는 냉동 보관',
       TRUE, featured, category_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    VALUES
      ('먹태 39~41cm 1kg 내외', 'meoktae-1kg', 35000, 37000, 24, '노릇하게 구워 고소한 먹태', '술안주와 간식으로 좋은 일품 먹태입니다.', TRUE, (SELECT id FROM categories WHERE slug='meoktae')),
      ('북어채 1kg', 'dried-pollack-1kg', 30000, 32000, 30, '국과 무침에 두루 쓰는 북어채', '먹기 좋게 찢어 손질한 담백한 북어채입니다.', TRUE, (SELECT id FROM categories WHERE slug='meoktae')),
      ('반건조 짝태 10미', 'jjagtae-10', 39000, 42000, 20, '구워 먹기 좋은 담백한 짝태', '먹기 좋게 손질해 술안주와 간식으로 활용하기 좋습니다.', TRUE, (SELECT id FROM categories WHERE slug='jjagtae')),
      ('반건조 오징어 10미', 'semi-dried-squid-10', 42000, 45000, 18, '쫄깃하고 촉촉한 반건조 오징어', '가정에서 간편하게 굽기 좋은 구성입니다.', TRUE, (SELECT id FROM categories WHERE slug='squid')),
      ('마른 오징어 5미', 'dried-squid-5', 38000, 40000, 15, '씹을수록 진한 감칠맛', '선별한 오징어를 깔끔하게 건조했습니다.', FALSE, (SELECT id FROM categories WHERE slug='squid')),
      ('완도 자른 미역 500g', 'wando-seaweed-500', 15000, 18000, 40, '깔끔하게 손질한 완도산 미역', '국과 냉국에 바로 사용하기 편합니다.', TRUE, (SELECT id FROM categories WHERE slug='miyeok')),
      ('무말랭이 장아찌 1kg', 'radish-pickle-1kg', 14000, 16000, 22, '오독오독한 밥도둑 반찬', '과하지 않은 양념으로 매일 먹기 좋습니다.', TRUE, (SELECT id FROM categories WHERE slug='jangajji')),
      ('건새우 500g', 'dried-shrimp-500', 15000, 20000, 35, '볶음과 육수에 좋은 건새우', '선별 후 깔끔하게 포장했습니다.', FALSE, (SELECT id FROM categories WHERE slug='processed')),
      ('고구마줄기 1kg', 'sweet-potato-stem-1kg', 15000, 25000, 20, '나물 요리에 좋은 건고구마줄기', '충분히 불려 각종 나물 요리에 활용하세요.', FALSE, (SELECT id FROM categories WHERE slug='processed'))
) AS seed(name, slug, price, original_price, stock, summary, description, featured, category_id)
ON CONFLICT (slug) DO NOTHING;
