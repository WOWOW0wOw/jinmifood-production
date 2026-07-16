-- 초기 개발 DB에 잘못된 문자 인코딩으로 저장된 상품 문구를 안정적인 slug 기준으로 복구한다.
UPDATE products
SET name = '북어채 1kg',
    summary = '국과 무침에 편리한 북어채',
    description = '먹기 좋게 찢어 손질한 담백한 북어채입니다.'
WHERE slug = 'dried-pollack-1kg';

UPDATE products
SET name = '무말랭이 장아찌 1kg',
    summary = '오독오독한 밥도둑 반찬',
    description = '과하지 않은 양념으로 매일 먹기 좋습니다.'
WHERE slug = 'radish-pickle-1kg';
