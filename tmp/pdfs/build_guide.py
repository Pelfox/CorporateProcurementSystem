from pathlib import Path
import re
import html
from reportlab.pdfgen import canvas
from reportlab.platypus import (
    BaseDocTemplate, PageTemplate, Frame, Paragraph, Spacer, PageBreak,
    Table, TableStyle, Preformatted, KeepTogether, Flowable
)
from reportlab.platypus.tableofcontents import TableOfContents
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'tmp/pdfs/guide.md'
OUTPUT = ROOT / 'output/pdf/java-spring-defense-guide.pdf'
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
FONT = Path('/System/Library/Fonts/Supplemental')
for name, filename in [('Body', 'Arial.ttf'), ('BodyBold', 'Arial Bold.ttf'),
                       ('BodyItalic', 'Arial Italic.ttf')]:
    pdfmetrics.registerFont(TTFont(name, str(FONT / filename)))
pdfmetrics.registerFont(TTFont('Mono', '/System/Library/Fonts/Menlo.ttc', subfontIndex=0))
pdfmetrics.registerFontFamily('Body', normal='Body', bold='BodyBold', italic='BodyItalic', boldItalic='BodyBold')
NAVY = colors.HexColor('#122B43')
TEAL = colors.HexColor('#007F83')
TEXT = colors.HexColor('#233644')
MUTED = colors.HexColor('#647787')
LIGHT = colors.HexColor('#EEF4F7')
W, H = A4
LEFT = 21 * mm
RIGHT = 19 * mm
WIDTH = W - LEFT - RIGHT

styles = {
    'body': ParagraphStyle('body', fontName='Body', fontSize=10.5, leading=15.3,
        textColor=TEXT, spaceAfter=7, splitLongWords=True, allowWidows=0, allowOrphans=0),
    'h1': ParagraphStyle('h1', fontName='BodyBold', fontSize=23, leading=28,
        textColor=NAVY, spaceAfter=16, keepWithNext=True),
    'h2': ParagraphStyle('h2', fontName='BodyBold', fontSize=13.2, leading=18,
        textColor=TEAL, spaceBefore=13, spaceAfter=7, keepWithNext=True),
    'h3': ParagraphStyle('h3', fontName='BodyBold', fontSize=11, leading=15.5,
        textColor=NAVY, spaceBefore=9, spaceAfter=5, keepWithNext=True),
    'bullet': ParagraphStyle('bullet', fontName='Body', fontSize=10.5, leading=15.1,
        textColor=TEXT, leftIndent=12, firstLineIndent=-10, spaceAfter=5,
        allowWidows=0, allowOrphans=0),
    'quote': ParagraphStyle('quote', fontName='Body', fontSize=10.2, leading=15,
        textColor=NAVY, borderColor=TEAL, borderWidth=0, borderPadding=9,
        backColor=LIGHT, spaceBefore=5, spaceAfter=10),
    'code': ParagraphStyle('code', fontName='Mono', fontSize=8.5, leading=12,
        textColor=NAVY, backColor=LIGHT, borderPadding=8, spaceBefore=5, spaceAfter=10),
    'cell': ParagraphStyle('cell', fontName='Body', fontSize=9.1, leading=12.6,
        textColor=TEXT, spaceAfter=0),
    'th': ParagraphStyle('th', fontName='BodyBold', fontSize=9.2, leading=12.5,
        textColor=colors.white),
    'source': ParagraphStyle('source', fontName='Body', fontSize=9, leading=13.5,
        textColor=MUTED, spaceAfter=7),
    'qa': ParagraphStyle('qa', fontName='Body', fontSize=10.3, leading=14.8,
        textColor=TEXT, spaceAfter=9, allowWidows=0, allowOrphans=0),
}

def inline(text):
    text = html.escape(text, quote=False)
    text = re.sub(r'\[([^\]]+)\]\((https?://[^)]+)\)',
                  lambda m: '<link href="' + m[2].replace('&', '&amp;') + '" color="#007F83">' + m[1] + '</link>', text)
    text = re.sub(r'`([^`]+)`', r'<font name="Mono" size="9">\1</font>', text)
    text = re.sub(r'\*\*([^*]+)\*\*', r'<b>\1</b>', text)
    return text

class Cover(Flowable):
    def __init__(self):
        Flowable.__init__(self)
        self.width = WIDTH
        self.height = 229 * mm
    def draw(self):
        c = self.canv
        c.setFillColor(NAVY)
        c.rect(-LEFT, -43*mm, W, H, fill=1, stroke=0)
        c.setFillColor(TEAL)
        c.rect(-LEFT, 14*mm, 6*mm, 215*mm, fill=1, stroke=0)
        def txt(x, y, text, font, size, color):
            c.setFillColor(color); c.setFont(font, size); c.drawString(x, y, text)
        txt(0, 214*mm, 'CORPORATE PROCUREMENT SYSTEM', 'BodyBold', 10, colors.HexColor('#81D9D7'))
        txt(0, 173*mm, 'Java и Spring', 'BodyBold', 38, colors.white)
        txt(0, 151*mm, 'Подготовка', 'BodyBold', 34, colors.white)
        txt(0, 134*mm, 'к защите проекта', 'BodyBold', 30, colors.white)
        c.setStrokeColor(colors.HexColor('#55738B')); c.line(0, 119*mm, WIDTH, 119*mm)
        for y, s in [(103, 'Теория · устройство проекта · практические задачи'),
                     (91, 'Java Core / ООП / JDBC / SQL'),
                     (79, 'Spring Framework / Boot / MVC / Security'),
                     (67, 'Spring Data / JPA / Hibernate / транзакции')]:
            txt(0, y*mm, s.replace('·', '/'), 'Body', 12, colors.HexColor('#D7E5EF'))
        txt(0, 27*mm, 'Расширенный конспект и банк вопросов с ответами', 'BodyBold', 11, colors.white)
        txt(0, 15*mm, 'Редакция от 19 ноября 2026 / код проекта a2524b5', 'Body', 9.5, colors.HexColor('#A9BFCE'))

class CodeBlock(Preformatted):
    def draw(self):
        c = self.canv
        c.saveState()
        c.setFillColor(LIGHT)
        c.roundRect(0, -5, self.width, self.height+10, 3, fill=1, stroke=0)
        c.translate(7, 0)
        super().draw()
        c.restoreState()

class GuideDoc(BaseDocTemplate):
    def __init__(self, filename):
        super().__init__(str(filename), pagesize=A4, leftMargin=LEFT, rightMargin=RIGHT,
                         topMargin=23*mm, bottomMargin=21*mm,
                         title='Java и Spring: подготовка к защите CorporateProcurementSystem',
                         author='Учебный конспект для защиты проекта',
                         subject='Java, JDBC, Spring Boot, Spring Data JPA, Hibernate, Security',
                         pageCompression=1)
        self.addPageTemplates(PageTemplate(id='normal', frames=[
            Frame(LEFT, 21*mm, WIDTH, H-44*mm, leftPadding=0, rightPadding=0,
                  topPadding=0, bottomPadding=0)], onPageEnd=self.page_decor))
        self.current_chapter = 'Содержание'
    def beforeDocument(self):
        self.current_chapter = 'Содержание'
    def page_decor(self, c, doc):
        if doc.page == 1:
            return
        c.saveState()
        c.setStrokeColor(colors.HexColor('#D6E2E8'))
        c.line(LEFT, H-16*mm, W-RIGHT, H-16*mm)
        c.setFillColor(MUTED); c.setFont('Body', 8)
        c.drawString(LEFT, H-12.2*mm, 'JAVA / SPRING / ЗАЩИТА ПРОЕКТА')
        c.drawRightString(W-RIGHT, H-12.2*mm, 'CorporateProcurementSystem')
        c.line(LEFT, 15.5*mm, W-RIGHT, 15.5*mm)
        label = self.current_chapter
        if len(label) > 73: label = label[:70] + '...'
        c.drawString(LEFT, 10.5*mm, label)
        c.setFont('BodyBold', 9); c.drawRightString(W-RIGHT, 10.5*mm, str(doc.page))
        c.restoreState()
    def afterFlowable(self, flowable):
        if isinstance(flowable, Paragraph) and getattr(flowable, '_chapter', False):
            txt = flowable.getPlainText()
            key = 'chapter_' + str(flowable._chapter_index)
            self.canv.bookmarkPage(key)
            self.canv.addOutlineEntry(txt, key, level=0, closed=False)
            self.notify('TOCEntry', (0, txt, self.page, key))
            self.current_chapter = txt

def make_table(lines):
    rows = [[s.strip() for s in row.strip().strip('|').split('|')] for row in lines]
    rows = [r for r in rows if not all(re.fullmatch(r':?-+:?', x) for x in r)]
    count = len(rows[0])
    if count == 2: widths = [WIDTH*0.29, WIDTH*0.71]
    elif count == 3: widths = [WIDTH*0.24, WIDTH*0.38, WIDTH*0.38]
    else: widths = [WIDTH/count]*count
    data = [[Paragraph(inline(x), styles['th' if i == 0 else 'cell']) for x in row] for i, row in enumerate(rows)]
    t = Table(data, colWidths=widths, repeatRows=1, hAlign='LEFT')
    t.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), NAVY),
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ('LEFTPADDING', (0,0), (-1,-1), 8), ('RIGHTPADDING', (0,0), (-1,-1), 8),
        ('TOPPADDING', (0,0), (-1,-1), 7), ('BOTTOMPADDING', (0,0), (-1,-1), 7),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, LIGHT]),
        ('LINEBELOW', (0,0), (-1,0), 0.5, NAVY),
        ('LINEBELOW', (0,1), (-1,-1), 0.25, colors.HexColor('#D6E2E8')),
    ]))
    return [t, Spacer(1, 8)]

def parse_markdown(text):
    lines = text.splitlines(); story = []; i = 0; chapter = 0
    while i < len(lines):
        line = lines[i].strip()
        if not line: i += 1; continue
        if line.startswith('```'):
            block=[]; i+=1
            while i<len(lines) and not lines[i].strip().startswith('```'):
                block.append(lines[i].rstrip()); i+=1
            # Protect the printed page width without changing semantics silently.
            for code in block:
                if pdfmetrics.stringWidth(code, 'Mono', 8.5) > WIDTH-18:
                    raise ValueError('Code line too wide: ' + code)
            story.append(CodeBlock('\n'.join(block), styles['code']))
            i+=1; continue
        if line.startswith('|'):
            block=[]
            while i<len(lines) and lines[i].strip().startswith('|'):
                block.append(lines[i]); i+=1
            story.extend(make_table(block)); continue
        if line.startswith('# '):
            chapter += 1; story.append(PageBreak())
            p = Paragraph(inline(line[2:]), styles['h1']); p._chapter=True; p._chapter_index=chapter
            story.append(p); i+=1; continue
        if line.startswith('## '):
            story.append(Paragraph(inline(line[3:]), styles['h2'])); i+=1; continue
        if line.startswith('### '):
            story.append(Paragraph(inline(line[4:]), styles['h3'])); i+=1; continue
        if line.startswith('> '):
            story.append(Paragraph(inline(line[2:]), styles['quote'])); i+=1; continue
        if line.startswith('- '):
            story.append(Paragraph('- ' + inline(line[2:]), styles['bullet'])); i+=1; continue
        paragraph=[line]; i+=1
        while i<len(lines) and lines[i].strip() and not re.match(r'^(#|>|-|\||```)', lines[i].strip()):
            paragraph.append(lines[i].strip()); i+=1
        s=' '.join(paragraph)
        st=styles['qa'] if re.match(r'\*\*\d+\.', s) else styles['body']
        story.append(Paragraph(inline(s), st))
    return story

def main():
    toc = TableOfContents()
    toc.levelStyles = [ParagraphStyle('toc0', fontName='Body', fontSize=10.5, leading=16,
                                     textColor=NAVY, spaceBefore=7, leftIndent=0,
                                     firstLineIndent=0, rightIndent=20)]
    toc.dotsMinLevel=0
    story=[Cover(), PageBreak(), Paragraph('Содержание', styles['h1']),
           Paragraph('Разделы доступны по ссылкам. В боковой панели PDF также есть закладки.', styles['body']), toc]
    story += parse_markdown(SOURCE.read_text(encoding='utf-8'))
    doc=GuideDoc(OUTPUT)
    doc.multiBuild(story)
    print(OUTPUT)

if __name__ == '__main__':
    main()
