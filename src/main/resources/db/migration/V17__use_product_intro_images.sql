UPDATE products
SET detail_image_url = REPLACE(detail_image_url, '-ai-detail.jpg', '-ai-intro.jpg'),
    updated_at = CURRENT_TIMESTAMP
WHERE detail_image_url LIKE '%-ai-detail.jpg';
