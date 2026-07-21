"""Build long-form product detail images from newly generated source photos.

The AI source photographs intentionally contain no copy. Korean text and the
layout are rendered here so every product page stays legible and maintainable.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


WIDTH = 860
HEIGHT = 6400
IVORY = "#F7F1E7"
PAPER = "#FFFDF8"
INK = "#2D2924"
MUTED = "#746C62"
ACCENT = "#8B4E2F"
OLIVE = "#606347"
LINE = "#DED4C6"


@dataclass(frozen=True)
class Product:
    asset_id: str
    name: str
    subtitle: str
    source: str
    cell: int
    points: tuple[str, str, str]
    uses: tuple[str, str, str]
    weight: str
    storage: str


PRODUCTS = (
    Product("1746168600", "북어채 1kg", "담백하고 활용도 높은 손질 북어채", "pollack.png", 0,
            ("먹기 좋은 결", "국·무침에 활용", "넉넉한 1kg 구성"), ("북엇국", "새콤한 무침", "간단한 볶음"), "1kg", "직사광선을 피해 서늘한 곳에 보관"),
    Product("1746168499", "알짝태 500g", "고소한 풍미를 담은 특별한 별미", "pollack.png", 1,
            ("선별한 원물", "쫀득한 식감", "술안주로 좋은 구성"), ("노릇하게 굽기", "양념구이", "마요 소스 곁들이기"), "500g", "냉장 또는 냉동 보관 권장"),
    Product("1746168229", "짝태 42cm급", "크기별로 선별한 반건조 짝태", "pollack.png", 2,
            ("큼직한 원물", "담백한 감칠맛", "간편한 구이용"), ("팬에 굽기", "에어프라이어", "매콤한 양념구이"), "약 1.1kg부터", "냉동 보관 후 필요한 만큼 해동"),
    Product("1746168000", "껍질 벗긴 짝태채 500g", "손질 부담 없이 간편하게", "pollack.png", 3,
            ("껍질 제거", "먹기 좋은 크기", "간식·안주 겸용"), ("그대로 즐기기", "살짝 구워 먹기", "양념 무침"), "500g", "밀봉 후 서늘한 곳 또는 냉장 보관"),
    Product("1746168960", "먹태 39~41cm 1kg 내외", "노릇하고 고소한 대표 안주", "pickles.png", 0,
            ("고소한 풍미", "쫄깃한 결", "선별한 크기"), ("약불에 굽기", "에어프라이어", "청양마요 소스"), "1kg 내외", "밀봉하여 냉동 보관 권장"),
    Product("1746169070", "고추장아찌 모둠 500g", "매콤새콤 입맛을 살리는 반찬", "pickles.png", 1,
            ("아삭한 식감", "균형 잡힌 양념", "바로 먹는 간편함"), ("따뜻한 밥과", "고기 요리 곁들임", "비빔밥 토핑"), "500g", "수령 후 냉장 보관"),
    Product("1746168838", "무말랭이 1kg", "오독오독 살아 있는 식감", "pickles.png", 2,
            ("깔끔한 건조", "오독한 식감", "다양한 반찬 활용"), ("충분히 불리기", "양념 무침", "장아찌 만들기"), "1kg", "직사광선을 피해 밀봉 보관"),
    Product("1746167513", "모둠 야채장아찌 500g", "다채로운 채소를 한 번에", "pickles.png", 3,
            ("여러 채소 구성", "아삭한 한입", "깔끔한 양념"), ("밥반찬", "고기와 곁들이기", "면 요리 반찬"), "500g", "수령 후 냉장 보관"),
    Product("1746168728", "미역 묶음 5kg", "줄기와 잎을 넉넉하게 담은 구성", "seaweed.png", 0,
            ("넉넉한 대용량", "깊은 바다 향", "다양한 조리"), ("미역국", "초무침", "쌈·볶음"), "5kg", "서늘하고 건조한 곳에 밀봉 보관"),
    Product("1746167889", "미역줄기 5kg", "꼬들꼬들 반찬용 미역줄기", "seaweed.png", 1,
            ("꼬들한 식감", "대용량 구성", "볶음·무침 활용"), ("염분 빼기", "들기름 볶음", "새콤한 무침"), "5kg", "서늘한 곳에 보관, 개봉 후 냉장"),
    Product("1746167773", "쌈미역 5kg", "넓고 탄탄한 잎으로 즐기는 바다의 맛", "seaweed.png", 2,
            ("넓은 잎", "탄탄한 식감", "쌈용으로 간편"), ("깨끗이 헹구기", "먹기 좋게 자르기", "초장과 곁들이기"), "5kg", "냉장 보관 후 빠른 섭취 권장"),
    Product("1746166839", "연한 미역줄기 5kg", "부드럽게 즐기는 미역줄기", "seaweed.png", 3,
            ("연한 식감", "손쉬운 조리", "넉넉한 구성"), ("찬물에 헹구기", "가볍게 볶기", "샐러드·무침"), "5kg", "냉장 보관"),
    Product("1746168372", "마른 오징어 10미", "씹을수록 진해지는 감칠맛", "squid.png", 0,
            ("선별한 10미", "쫄깃한 식감", "진한 풍미"), ("약불에 굽기", "버터구이", "마요 소스"), "10마리", "밀봉하여 냉동 보관"),
    Product("1746167375", "반건조 꼴뚜기", "촉촉하고 쫄깃한 한입 별미", "squid.png", 1,
            ("촉촉한 반건조", "한입 크기", "간편한 조리"), ("팬 구이", "버터 볶음", "매콤한 볶음"), "500g 단위 구성", "수령 즉시 냉동 보관"),
    Product("1746166486", "오징어 실채 1kg", "가늘고 부드러운 오징어채", "squid.png", 2,
            ("먹기 좋은 실채", "은은한 단맛", "반찬·간식 겸용"), ("간장 볶음", "고추장 무침", "그대로 간식"), "500g × 2봉", "밀봉하여 서늘한 곳 또는 냉장 보관"),
    Product("1746166104", "손질 냉동 오징어입 2kg", "손질 부담을 줄인 쫄깃한 별미", "squid.png", 3,
            ("입·내장 손질", "쫄깃한 식감", "소분하기 좋은 구성"), ("숙회", "매콤 볶음", "버터구이"), "1kg × 2봉", "수령 즉시 냉동 보관"),
    Product("1746169275", "건새우 500g", "볶음과 육수에 두루 쓰는 건새우", "pantry.png", 0,
            ("선명한 원물", "고소한 감칠맛", "다양한 활용"), ("마른 팬 볶음", "국물 육수", "밑반찬"), "500g", "밀봉하여 냉동 보관 권장"),
    Product("1746169174", "건고구마줄기 1kg", "정성껏 불려 즐기는 구수한 나물", "pantry.png", 1,
            ("깔끔한 건조", "구수한 식감", "나물용 대용량"), ("충분히 불리기", "부드럽게 삶기", "들깨 나물"), "1kg", "직사광선을 피해 건조 보관"),
    Product("1746168110", "청건고추 1kg", "요리에 개운한 매운맛을 더하는 재료", "pantry.png", 2,
            ("깔끔한 건조", "은은한 매운맛", "육수·양념 활용"), ("육수에 넣기", "양념장 만들기", "볶음 요리"), "1kg", "밀봉하여 서늘하고 건조한 곳에 보관"),
    Product("1746167666", "쏸라펀 12개입", "새콤하고 얼얼한 당면 한 그릇", "pantry.png", 3,
            ("간편한 한 끼", "쫄깃한 당면", "새콤매콤한 국물"), ("표시선까지 물", "충분히 익히기", "고명 추가하기"), "12개입", "직사광선을 피해 실온 보관"),
    Product("1746167191", "마라가재 700g × 3", "얼얼한 풍미와 탱글한 식감", "crawfish-wrappers.png", 0,
            ("진한 마라 풍미", "탱글한 가재", "넉넉한 3팩"), ("중탕 데우기", "팬에 볶기", "면 사리 곁들이기"), "700g × 3팩", "수령 즉시 냉동 보관"),
    Product("1746166995", "춘권피·춘병 5봉 구성", "얇고 유연해 다양한 요리에", "crawfish-wrappers.png", 1,
            ("얇고 유연한 피", "두 가지 형태", "넉넉한 구성"), ("춘권 만들기", "채소말이", "간편한 전병"), "5봉 혼합 구성", "제품 표시사항에 따라 냉장·냉동 보관"),
)


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    path = Path("C:/Windows/Fonts/malgunbd.ttf" if bold else "C:/Windows/Fonts/malgun.ttf")
    return ImageFont.truetype(str(path), size)


def crop_cell(sheet: Image.Image, cell: int) -> Image.Image:
    w, h = sheet.size
    if w / h < 0.7:  # two-column portrait sheet
        half = w // 2
        box = (0 if cell == 0 else half, 0, half if cell == 0 else w, h)
    else:
        half_w, half_h = w // 2, h // 2
        col, row = cell % 2, cell // 2
        box = (col * half_w, row * half_h, (col + 1) * half_w, (row + 1) * half_h)
    return sheet.crop(box)


def cover(image: Image.Image, size: tuple[int, int], focus_y: float = 0.5) -> Image.Image:
    tw, th = size
    scale = max(tw / image.width, th / image.height)
    nw, nh = round(image.width * scale), round(image.height * scale)
    resized = image.resize((nw, nh), Image.Resampling.LANCZOS)
    left = max(0, (nw - tw) // 2)
    top = max(0, min(nh - th, round((nh - th) * focus_y)))
    return resized.crop((left, top, left + tw, top + th))


def centered(draw: ImageDraw.ImageDraw, y: int, text: str, fnt: ImageFont.FreeTypeFont, fill: str) -> None:
    box = draw.textbbox((0, 0), text, font=fnt)
    draw.text(((WIDTH - (box[2] - box[0])) / 2, y), text, font=fnt, fill=fill)


def round_rect(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], radius: int = 24,
               fill: str = PAPER, outline: str | None = None, width: int = 1) -> None:
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def make_detail(product: Product, source_dir: Path, output_dir: Path) -> Path:
    sheet = Image.open(source_dir / product.source).convert("RGB")
    photo = crop_cell(sheet, product.cell)
    canvas = Image.new("RGB", (WIDTH, HEIGHT), IVORY)
    draw = ImageDraw.Draw(canvas)

    # Opening statement
    centered(draw, 86, "JINMI FOOD · SELECT", font(22, True), ACCENT)
    centered(draw, 150, product.name, font(52, True), INK)
    centered(draw, 232, product.subtitle, font(25), MUTED)
    draw.line((330, 298, 530, 298), fill=ACCENT, width=3)

    hero = cover(photo, (760, 900), 0.45)
    canvas.paste(hero, (50, 350))
    draw.rectangle((50, 350, 810, 1250), outline=PAPER, width=8)

    centered(draw, 1325, "매일의 식탁에 좋은 재료를 고릅니다", font(30, True), INK)
    centered(draw, 1380, "원물의 맛과 쓰임을 생각한 진미푸드의 선택", font(21), MUTED)

    # Three product reasons
    centered(draw, 1515, "이 상품을 추천하는 이유", font(36, True), INK)
    for index, point in enumerate(product.points, start=1):
        x = 55 + (index - 1) * 255
        round_rect(draw, (x, 1600, x + 240, 1885), 22, PAPER, LINE, 2)
        draw.ellipse((x + 90, 1640, x + 150, 1700), fill=ACCENT)
        centered_x = x + 120
        nbox = draw.textbbox((0, 0), str(index), font=font(24, True))
        draw.text((centered_x - (nbox[2] - nbox[0]) / 2, 1650), str(index), font=font(24, True), fill="white")
        pbox = draw.textbbox((0, 0), point, font=font(22, True))
        draw.text((centered_x - (pbox[2] - pbox[0]) / 2, 1750), point, font=font(22, True), fill=INK)
        draw.text((centered_x - 68, 1800), "꼼꼼하게 담았습니다", font=font(16), fill=MUTED)

    # Texture / detail photo
    draw.rectangle((0, 1990, WIDTH, 3100), fill="#E7DDD0")
    detail = cover(photo, (720, 870), 0.70)
    canvas.paste(detail, (70, 2110))
    draw.rectangle((70, 2110, 790, 2980), outline=PAPER, width=8)
    centered(draw, 2032, "NATURAL TEXTURE", font(20, True), ACCENT)
    centered(draw, 3020, "사진으로 먼저 확인하는 원물의 결", font(25, True), INK)

    # Serving ideas
    centered(draw, 3245, "맛있게 즐기는 방법", font(38, True), INK)
    centered(draw, 3310, "복잡하지 않게, 익숙한 식탁에 더해보세요", font(21), MUTED)
    for index, use in enumerate(product.uses, start=1):
        y = 3425 + (index - 1) * 285
        draw.ellipse((75, y, 155, y + 80), fill=OLIVE)
        nbox = draw.textbbox((0, 0), f"0{index}", font=font(24, True))
        draw.text((115 - (nbox[2] - nbox[0]) / 2, y + 22), f"0{index}", font=font(24, True), fill="white")
        draw.text((190, y + 4), use, font=font(29, True), fill=INK)
        draw.text((190, y + 54), "기호와 조리 환경에 맞게 시간을 조절해 주세요.", font=font(19), fill=MUTED)
        draw.line((190, y + 115, 785, y + 115), fill=LINE, width=2)

    # Product information
    draw.rectangle((0, 4335, WIDTH, 5145), fill=PAPER)
    centered(draw, 4420, "상품 정보", font(38, True), INK)
    rows = (
        ("상품명", product.name),
        ("내용량", product.weight),
        ("보관방법", product.storage),
        ("판매원", "진미푸드"),
    )
    for index, (label, value) in enumerate(rows):
        y = 4530 + index * 135
        draw.text((90, y), label, font=font(21, True), fill=ACCENT)
        draw.text((250, y), value, font=font(21), fill=INK)
        draw.line((90, y + 70, 770, y + 70), fill=LINE, width=2)
    draw.text((90, 5065), "※ 정확한 원산지·소비기한·보관 표시는 수령한 상품의 표기사항을 우선 확인해 주세요.", font=font(15), fill=MUTED)

    # Storage and delivery notes
    centered(draw, 5275, "받으신 뒤 꼭 확인해 주세요", font(36, True), INK)
    notes = (
        ("01", "보관", product.storage),
        ("02", "배송", "신선도 유지를 위해 수령 즉시 상품 상태를 확인해 주세요."),
        ("03", "문의", "이상 발견 시 섭취·폐기 전에 사진과 함께 고객센터로 문의해 주세요."),
    )
    for index, (number, title, body) in enumerate(notes):
        y = 5380 + index * 205
        round_rect(draw, (70, y, 790, y + 165), 18, "#EEE5D9")
        draw.text((100, y + 38), number, font=font(22, True), fill=ACCENT)
        draw.text((175, y + 34), title, font=font(24, True), fill=INK)
        draw.text((285, y + 39), body, font=font(17), fill=MUTED)

    draw.rectangle((0, 6080, WIDTH, HEIGHT), fill="#33362C")
    centered(draw, 6160, "진미푸드", font(34, True), "#F8F1E7")
    centered(draw, 6220, "좋은 재료를 정직하게 전합니다", font(20), "#D8CEBF")
    centered(draw, 6280, "JINMIFOOD.COM", font(16, True), "#BDAF9E")

    output_dir.mkdir(parents=True, exist_ok=True)
    output = output_dir / f"{product.asset_id}-crafted-detail.jpg"
    canvas.save(output, "JPEG", quality=88, optimize=True, progressive=True, subsampling=1)
    return output


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-dir", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args()
    for product in PRODUCTS:
        output = make_detail(product, args.source_dir, args.output_dir)
        print(output)


if __name__ == "__main__":
    main()
