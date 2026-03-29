#!/usr/bin/env python3
"""Modern clean SVG wireframe mockups with rich visualizations."""
import os, math

OUT_DIR = '/Users/zhaoyu/Desktop/UI_Mockups'
os.makedirs(OUT_DIR, exist_ok=True)

# Modern color palette
C = {
    'primary': '#4F46E5',      # Indigo - primary action
    'primary_light': '#818CF8',
    'primary_bg': '#EEF2FF',
    'teal': '#0D9488',          # Teal accent
    'teal_bg': '#CCFBF1',
    'orange': '#EA580C',
    'orange_bg': '#FFF7ED',
    'green': '#16A34A',
    'green_bg': '#DCFCE7',
    'red': '#DC2626',
    'red_bg': '#FEE2E2',
    'purple': '#7C3AED',
    'purple_bg': '#EDE9FE',
    'navy': '#0F172A',          # Dark header
    'sidebar': '#1E293B',
    'text': '#1E293B',
    'text_secondary': '#64748B',
    'text_light': '#94A3B8',
    'border': '#E2E8F0',
    'bg': '#F1F5F9',
    'card': '#FFFFFF',
    'card_hover': '#F8FAFC',
    'shadow': 'rgba(15,23,42,0.08)',
}

def svg(w, h):
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">\n'

def defs():
    return '''<defs>
<filter id="shadow-sm"><feDropShadow dx="0" dy="1" stdDeviation="3" flood-color="rgba(15,23,42,0.08)"/></filter>
<filter id="shadow-md"><feDropShadow dx="0" dy="4" stdDeviation="8" flood-color="rgba(15,23,42,0.10)"/></filter>
<filter id="shadow-lg"><feDropShadow dx="0" dy="8" stdDeviation="16" flood-color="rgba(15,23,42,0.12)"/></filter>
<linearGradient id="grad-primary" x1="0" y1="0" x2="1" y2="1">
<stop offset="0%" stop-color="#6366F1"/><stop offset="100%" stop-color="#4F46E5"/></linearGradient>
<linearGradient id="grad-teal" x1="0" y1="0" x2="1" y2="1">
<stop offset="0%" stop-color="#14B8A6"/><stop offset="100%" stop-color="#0D9488"/></linearGradient>
<linearGradient id="grad-green" x1="0" y1="0" x2="1" y2="1">
<stop offset="0%" stop-color="#22C55E"/><stop offset="100%" stop-color="#16A34A"/></linearGradient>
</defs>\n'''

def R(x, y, w, h, r=12, fill=C['card'], stroke=C['border'], sw=1, shadow=False):
    blur = 'filter="url(#shadow-sm)"' if shadow else ''
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}" {blur}/>\n'

def T(x, y, txt, fill=C['text'], sz=13, bold=False, anchor='start', family='PingFang SC,Helvetica,Arial'):
    w = 'font-weight="600"' if bold else ''
    return f'<text x="{x}" y="{y}" font-family="{family}" font-size="{sz}" fill="{fill}" text-anchor="{anchor}" {w}>{txt}</text>\n'

def I(x, y, w, h, fill=C['bg'], stroke=C['border'], r=8):
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}" stroke="{stroke}"/>\n'

def L(x1, y1, x2, y2, stroke=C['border'], sw=1, dash=None):
    d = f'stroke-dasharray="{dash}"' if dash else ''
    return f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{stroke}" stroke-width="{sw}" {d}/>\n'

def tag(x, y, txt, bg=C['primary_bg'], fg=C['primary'], h=24, r=6):
    ww = len(txt)*13+16
    return f'<rect x="{x}" y="{y}" width="{ww}" height="{h}" rx="{r}" fill="{bg}"/>\n' + \
           T(x+8, y+16, txt, fg, 11, True)

def dot(cx, cy, r, fill):
    return f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{fill}"/>\n'

def legend_item(x, y, color, label, sz=11):
    return dot(x, y, 5, color) + T(x+12, y+4, label, C['text_secondary'], sz)

# ---- Area chart ----
def area_chart(x, y, w, h, data, color, label_y=True):
    pts = data
    n = len(pts)
    max_v = max(pts) * 1.1
    min_v = 0
    xs = [x + i*w/(n-1) for i in range(n)]
    ys = [y+h - (v-min_v)/(max_v-min_v)*h for v in pts]
    area_pts = f'{x},{y+h} ' + ' '.join([f'{xi},{yi}' for xi,yi in zip(xs,ys)]) + f' {x+w},{y+h}'
    line_pts = ' '.join([f'{xi},{yi}' for xi,yi in zip(xs,ys)])
    area = f'<polygon points="{area_pts}" fill="{color}" opacity="0.15"/>\n'
    line = f'<polyline points="{line_pts}" fill="none" stroke="{color}" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>\n'
    # dots
    dots = ''
    for xi, yi in zip(xs, ys):
        dots += f'<circle cx="{xi}" cy="{yi}" r="4" fill="{color}"/>\n'
        dots += f'<circle cx="{xi}" cy="{yi}" r="6" fill="none" stroke="{color}" stroke-width="1.5" opacity="0.3"/>\n'
    # grid lines
    grid = ''
    for i in range(5):
        gy = y + i*h/4
        grid += L(x, gy, x+w, gy, C['border'], 0.5, '4,4')
    return grid + area + line + dots

# ---- Bar chart ----
def bar_chart(x, y, w, h, labels, values, colors, show_values=True):
    n = len(labels)
    bw = min(40, (w-20)/n-8)
    gap = (w - n*bw) / (n+1)
    result = ''
    max_v = max(values) * 1.15
    for i, (lbl, val, col) in enumerate(zip(labels, values, colors)):
        bx = x + gap + i*(bw+gap)
        bh = val/max_v * (h-40)
        by = y+h-30-bh
        # bar
        result += f'<rect x="{bx}" y="{by}" width="{bw}" height="{bh}" rx="6" fill="{col}"/>\n'
        # value on top
        if show_values:
            result += T(bx+bw//2, by-6, f'{val}', C['text'], 10, True, 'middle')
        # label below
        result += T(bx+bw//2, y+h-12, lbl, C['text_secondary'], 10, False, 'middle')
    # baseline
    result += L(x, y+h-30, x+w, y+h-30, C['border'], 1)
    return result

# ---- Radar chart ----
def radar_chart(cx, cy, r, axes_data, colors):
    n = len(axes_data[0])
    result = f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="none" stroke="{C["border"]}" stroke-width="1"/>\n'
    for frac in [0.25, 0.5, 0.75, 1.0]:
        rr = r * frac
        pts = []
        for i in range(n):
            angle = math.pi*2*i/n - math.pi/2
            px = cx + rr * math.cos(angle)
            py = cy + rr * math.sin(angle)
            pts.append((px, py))
        pts_str = ' '.join([f'{px:.1f},{py:.1f}' for px,py in pts])
        result += f'<polygon points="{pts_str}" fill="none" stroke="{C["border"]}" stroke-width="0.5" opacity="0.5"/>\n'
    # axis lines
    for i in range(n):
        angle = math.pi*2*i/n - math.pi/2
        px = cx + r * math.cos(angle)
        py = cy + r * math.sin(angle)
        result += L(cx, cy, px, py, C['border'], 0.5)
    # data polygons
    for ki, (data, col) in enumerate(axes_data):
        pts = []
        for i, v in enumerate(data):
            angle = math.pi*2*i/n - math.pi/2
            px = cx + r*v * math.cos(angle)
            py = cy + r*v * math.sin(angle)
            pts.append((px, py))
        pts_str = ' '.join([f'{px:.1f},{py:.1f}' for px,py in pts])
        result += f'<polygon points="{pts_str}" fill="{col}" opacity="0.2" stroke="{col}" stroke-width="2"/>\n'
        for px, py in pts:
            result += f'<circle cx="{px:.1f}" cy="{py:.1f}" r="3" fill="{col}"/>\n'
    return result

# ---- Gauge chart ----
def gauge(cx, cy, r, value, color, label=''):
    start_angle = math.pi * 0.75
    end_angle = math.pi * 0.25
    arc_angle = start_angle - (start_angle - end_angle) * value
    # background arc
    sx = cx + r * math.cos(start_angle)
    sy = cy - r * math.sin(start_angle)
    ex = cx + r * math.cos(end_angle)
    ey = cy - r * math.sin(end_angle)
    large = 1 if value > 0.5 else 0
    bg_arc = f'<path d="M {sx:.1f} {sy:.1f} A {r} {r} 0 {large} 1 {ex:.1f} {ey:.1f}" fill="none" stroke="{C["border"]}" stroke-width="12" stroke-linecap="round"/>\n'
    # value arc
    vx = cx + r * math.cos(arc_angle)
    vy = cy - r * math.sin(arc_angle)
    val_arc = f'<path d="M {sx:.1f} {sy:.1f} A {r} {r} 0 {large} 1 {vx:.1f} {vy:.1f}" fill="none" stroke="{color}" stroke-width="12" stroke-linecap="round"/>\n'
    # center text
    pct = T(cx, cy-4, f'{int(value*100)}', color, 28, True, 'middle')
    unit = T(cx, cy+18, label, C['text_secondary'], 12, False, 'middle')
    return bg_arc + val_arc + pct + unit

# ---- Horizontal bar with label ----
def hbar_group(x, y, w, h, items, max_val, colors):
    row_h = h / len(items)
    result = ''
    for i, (label, val, col) in enumerate(items):
        iy = y + i*row_h
        bw = val/max_val * (w-80)
        result += T(x, iy+row_h/2+4, label, C['text_secondary'], 11, False, 'start')
        result += f'<rect x="{x+80}" y="{iy+row_h/2-10}" width="{bw}" height="20" rx="4" fill="{col}"/>\n'
        result += T(x+80+bw+8, iy+row_h/2+4, f'{val}', col, 11, True)
    return result

# ---- Sparkline ----
def sparkline(x, y, w, h, data, color):
    n = len(data)
    max_v, min_v = max(data), min(data)
    rng = max_v - min_v or 1
    pts = ' '.join([f'{x+i*w/(n-1):.1f},{y+h-(v-min_v)/rng*h:.1f}' for i,v in enumerate(data)])
    area = f'<polygon points="{x},{y+h} ' + ' '.join([f'{x+i*w/(n-1):.1f},{y+h-(v-min_v)/rng*h:.1f}' for i,v in enumerate(data)]) + f' {x+w},{y+h}" fill="{color}" opacity="0.15"/>\n'
    line = f'<polyline points="{pts}" fill="none" stroke="{color}" stroke-width="2" stroke-linecap="round"/>\n'
    return area + line

# ============================================================
# PAGE 1: Login
# ============================================================
def page_login():
    W, H = 1200, 800
    c = svg(W, H)
    c += defs()
    # Background gradient effect
    c += f'<rect width="{W}" height="{H}" fill="{C["bg"]}"/>\n'
    # Decorative blobs
    c += f'<ellipse cx="200" cy="200" rx="400" ry="350" fill="{C["primary"]}" opacity="0.05"/>\n'
    c += f'<ellipse cx="1000" cy="600" rx="400" ry="350" fill="{C["teal"]}" opacity="0.05"/>\n'
    # Card
    card_x, card_y, card_w, card_h = 380, 160, 440, 480
    c += R(card_x, card_y, card_w, card_h, 20, C['card'], 'none', 0, shadow=True)
    # Logo area
    c += f'<rect x="{card_x+30}" y="{card_y+30}" width="48" height="48" rx="14" fill="url(#grad-primary)"/>\n'
    c += T(card_x+95, card_y+58, '机构绩效管理', C['text'], 20, True)
    c += T(card_x+95, card_y+78, 'Institution Performance Management', C['text_light'], 10)
    c += L(card_x+30, card_y+110, card_x+card_w-30, card_y+110, C['border'], 1)
    # Welcome text
    c += T(card_x+30, card_y+140, '欢迎回来', C['text'], 22, True)
    c += T(card_x+30, card_y+163, '请登录您的账号', C['text_secondary'], 13)
    # Form
    c += T(card_x+30, card_y+200, '工号', C['text_secondary'], 12)
    c += I(card_x+30, card_y+215, card_w-60, 48, C['bg'], C['border'], 10)
    c += T(card_x+48, card_y+243, '请输入工号', C['text_light'], 13)
    c += T(card_x+30, card_y+285, '密码', C['text_secondary'], 12)
    c += I(card_x+30, card_y+300, card_w-60, 48, C['bg'], C['border'], 10)
    c += T(card_x+48, card_y+328, '请输入密码', C['text_light'], 13)
    # Login button
    c += f'<rect x="{card_x+30}" y="{card_y+375}" width="{card_w-60}" height="50" rx="12" fill="url(#grad-primary)" filter="url(#shadow-sm)"/>\n'
    c += T(card_x+card_w//2, card_y+405, '登 录', C['card'], 15, True, 'middle')
    # Footer
    c += T(card_x+card_w//2, card_y+card_h-30, '机构绩效管理系统 V1.0.0 ·  忘记密码？', C['text_light'], 11, False, 'middle')
    c += footer()
    write('01_login', c)

# ============================================================
# PAGE 2: Dashboard
# ============================================================
def page_dashboard():
    W, H = 1200, 800
    c = svg(W, H)
    c += defs()
    c += f'<rect width="{W}" height="{H}" fill="{C["bg"]}"/>\n'
    # Header
    c += f'<rect width="{W}" height="64" fill="{C["navy"]}"/>\n'
    c += f'<rect x="24" y="18" width="36" height="28" rx="8" fill="{C["primary"]}"/>\n'
    c += T(72, 38, '绩效管理系统', C['card'], 16, True)
    c += T(700, 38, '首页仪表盘', C['card'], 13, False, 'middle')
    c += T(820, 38, '月度考核监测', C['text_light'], 13)
    c += T(940, 38, '绩效结果展示', C['text_light'], 13)
    # Avatar
    c += f'<rect x="1060" y="14" width="36" height="36" rx="18" fill="{C["primary"]}"/>\n'
    c += T(1078, 36, '张', C['card'], 14, True)
    c += T(1012, 38, '管理员 张三', C['card'], 13)
    # Sidebar
    c += f'<rect width="220" height="{H-64}" y="64" fill="{C["sidebar"]}"/>\n'
    items = ['仪表盘', '考核体系', '月度监测', '绩效报表', '通知中心']
    icons = ['◈', '◇', '◉', '▣', '◔']
    for i, (icon, item) in enumerate(zip(icons, items)):
        y = 100 + i*56
        if i == 0:
            c += f'<rect x="12" y="{y}" width="196" height="44" rx="10" fill="{C["primary"]}" opacity="0.15"/>\n'
            c += f'<rect x="12" y="{y}" width="4" height="44" rx="2" fill="{C["primary"]}"/>\n'
        c += T(36, y+26, icon, C['primary'] if i==0 else C['text_light'], 16, False)
        c += T(60, y+27, item, C['primary'] if i==0 else C['card'], 14, i==0)
    # Content
    cx = 260
    c += T(cx+20, 100, '今日概览', C['text'], 20, True)
    c += T(cx+780, 100, '2026年3月  ·  周五', C['text_light'], 13, False, 'end')
    # KPI Cards
    kpis = [
        (C['primary'], C['primary_bg'], '进行中监测', '3', '↑ 2', '+2 vs 上月', '◈'),
        (C['teal'], C['teal_bg'], '待确认', '5', '↑ 3', '+3 vs 上月', '◉'),
        (C['green'], C['green_bg'], '已发布结果', '24', '— 0', '持平上月', '✓'),
        (C['orange'], C['orange_bg'], '我的待办', '7', '↓ -2', '-2 vs 上周', '◔'),
    ]
    for i, (col, bg, title, num, trend, sub, icon) in enumerate(kpis):
        x = cx + 20 + i*230
        y = 120
        c += R(x, y, 210, 130, 16, C['card'], 'none', shadow=True)
        c += R(x, y, 4, 130, 2, col)  # accent bar
        c += T(x+20, y+24, title, C['text_secondary'], 12)
        c += T(x+20, y+62, num, col, 36, True)
        c += T(x+85, y+67, trend, C['green'] if trend[0]=='↑' else C['red'] if trend[0]=='↓' else C['text_light'], 13)
        c += T(x+20, y+105, sub, C['text_light'], 11)
        c += f'<rect x="{x+155}" y="{y+20}" width="40" height="40" rx="10" fill="{bg}"/>\n'
        c += T(x+175, y+45, icon, col, 18, False, 'middle')
    # Charts row
    y2 = 280
    # Trend chart (area)
    c += R(cx+20, y2, 560, 250, 16, C['card'], 'none', shadow=True)
    c += T(cx+40, y2+28, '绩效得分趋势', C['text'], 15, True)
    c += T(cx+440, y2+28, '近6月', C['text_light'], 12)
    c += legend_item(cx+40, y2+55, C['primary'], '北京分行')
    c += legend_item(cx+160, y2+55, C['teal'], '上海分行')
    c += legend_item(cx+280, y2+55, C['orange'], '深圳分行')
    # Area charts
    data1 = [0.6,0.72,0.65,0.8,0.85,0.92]
    data2 = [0.55,0.6,0.68,0.72,0.78,0.88]
    data3 = [0.5,0.55,0.58,0.65,0.7,0.85]
    c += area_chart(cx+40, y2+75, 520, 150, [v*100 for v in data1], C['primary'])
    c += area_chart(cx+40, y2+75, 520, 150, [v*100 for v in data2], C['teal'])
    c += area_chart(cx+40, y2+75, 520, 150, [v*100 for v in data3], C['orange'])
    # X axis labels
    for i, lbl in enumerate(['9月','10月','11月','12月','1月','2月']):
        c += T(cx+40+i*104, y2+232, lbl, C['text_light'], 10, False, 'middle')
    # Right: Dimension breakdown
    c += R(cx+600, y2, 560, 250, 16, C['card'], 'none', shadow=True)
    c += T(cx+620, y2+28, '维度得分分布', C['text'], 15, True)
    # Radar
    c += radar_chart(cx+780, y2+140, 80, [
        [0.92,0.85,0.88,0.90],
        [0.85,0.80,0.82,0.84],
    ], [C['primary'], C['teal']])
    c += T(cx+900, y2+40, '业务发展', C['text_secondary'], 11)
    c += T(cx+990, y2+100, '风险控制', C['text_secondary'], 11)
    c += T(cx+960, y2+200, '服务质量', C['text_secondary'], 11)
    c += T(cx+860, y2+200, '合规管理', C['text_secondary'], 11)
    legend_item(cx+620, y2+230, C['primary'], '北京分行')
    legend_item(cx+740, y2+230, C['teal'], '上海分行')
    # Bottom: Group comparison
    y3 = 550
    c += R(cx+20, y3, 1140, 220, 16, C['card'], 'none', shadow=True)
    c += T(cx+40, y3+28, '分组机构对比', C['text'], 15, True)
    c += tag(cx+180, y3+18, '北方区', C['primary_bg'], C['primary'], 22)
    c += tag(cx+250, y3+18, '华东区', C['teal_bg'], C['teal'], 22)
    c += tag(cx+320, y3+18, '华南区', C['orange_bg'], C['orange'], 22)
    bars_data = [
        ('北京分行', 92.3, C['primary']), ('天津分行', 87.5, C['primary']),
        ('上海分行', 88.9, C['teal']), ('杭州分行', 82.1, C['teal']),
        ('深圳分行', 85.6, C['orange']), ('广州分行', 79.3, C['orange']),
    ]
    for i, (lbl, val, col) in enumerate(bars_data):
        ix = cx+40+i*185
        iy = y3+60
        c += T(ix, iy+14, lbl, C['text_secondary'], 11)
        c += f'<rect x="{ix}" y="{iy+22}" width="{val*4.8}" height="28" rx="6" fill="{col}"/>\n'
        c += T(ix+val*4.8+8, iy+40, f'{val}', col, 13, True)
    # Recent activity
    c += T(cx+40, y3+125, '最近动态', C['text'], 13, True)
    for i, act in enumerate([
        '09:30  管理员发起了2026年3月的月度监测',
        '10:15  王五提交了北京分行存款数据',
        '11:00  李四确认了上海分行绩效数据',
        '12:00  管理员发布了2026年2月绩效结果',
    ]):
        c += T(cx+40, y3+148+i*18, act, C['text_secondary'], 11)
    c += footer()
    write('02_dashboard', c)

# ============================================================
# PAGE 3: Report Page - Rich Visualization
# ============================================================
def page_report():
    W, H = 1200, 900
    c = svg(W, H)
    c += defs()
    c += f'<rect width="{W}" height="{H}" fill="{C["bg"]}"/>\n'
    # Header
    c += f'<rect width="{W}" height="64" fill="{C["navy"]}"/>\n'
    c += f'<rect x="24" y="18" width="36" height="28" rx="8" fill="{C["primary"]}"/>\n'
    c += T(72, 38, '绩效管理系统', C['card'], 16, True)
    c += T(700, 38, '月度考核监测', C['text_light'], 13)
    c += T(820, 38, '绩效结果展示', C['primary'], 13)
    c += f'<rect x="940" y="14" width="36" height="36" rx="18" fill="{C["primary"]}"/>\n'
    c += T(958, 36, '张', C['card'], 14, True)
    c += T(990, 38, '管理员 张三', C['card'], 13)
    # Sidebar
    c += f'<rect width="220" height="{H-64}" y="64" fill="{C["sidebar"]}"/>\n'
    items = ['仪表盘', '考核体系', '月度监测', '绩效报表', '通知中心']
    for i, item in enumerate(items):
        y = 100 + i*56
        if i == 3:
            c += f'<rect x="12" y="{y}" width="196" height="44" rx="10" fill="{C["primary"]}" opacity="0.15"/>\n'
            c += f'<rect x="12" y="{y}" width="4" height="44" rx="2" fill="{C["primary"]}"/>\n'
        c += T(36, y+27, '◈◇◉▣◔'[i], C['primary'] if i==3 else C['text_light'], 16)
        c += T(60, y+27, item, C['primary'] if i==3 else C['card'], 14, i==3)
    # Theme config button
    c += R(20, H-100, 180, 44, 10, C['sidebar'], C['border'], 1)
    c += T(40, H-72, '🎨 主题配色设置', C['card'], 12)
    # Content
    cx = 260
    # Page title
    c += T(cx+20, 100, '绩效结果展示', C['text'], 22, True)
    c += T(cx+790, 100, '2026年3月  ·  2026年度分行绩效考核体系', C['text_light'], 13, False, 'end')
    # Filters
    yf = 120
    filter_items = [
        (cx+20, '2026年度分行绩效考核体系', C['bg']),
        (cx+280, '2026年', C['bg']),
        (cx+400, '3月', C['bg']),
        (cx+480, '全部机构', C['bg']),
        (cx+600, '全部分组', C['bg']),
    ]
    for fx, fval, fbg in filter_items:
        c += I(fx, yf, 150, 36, fbg, C['border'], 8)
        c += T(fx+12, yf+22, fval, C['text_secondary'], 12)
        c += T(fx+126, yf+22, '▼', C['text_light'], 10, False, 'middle')
    c += I(cx+770, yf, 60, 36, 'transparent', 'none')
    c += T(cx+770, yf+22, '⊕ 导出', C['primary'], 13, True)
    c += T(cx+860, yf+22, '⊕ 下载报告', C['teal'], 13, True)
    # Tab pills
    tabs = [('总览分析', 0), ('趋势分析', 1), ('维度分析', 2), ('分组对比', 3), ('指标完成度', 4)]
    for i, (tab, idx) in enumerate(tabs):
        tx = cx+20+i*120
        if i == 0:
            c += f'<rect x="{tx}" y="{yf+56}" width="110" height="36" rx="10" fill="{C["primary"]}"/>\n'
            c += T(tx+55, yf+78, tab, C['card'], 13, True, 'middle')
        else:
            c += T(tx+55, yf+78, tab, C['text_secondary'], 13, False, 'middle')
    yc = yf+110
    # Row 1: KPI summary cards
    kpis = [
        ('平均得分', '85.6', C['primary'], C['primary_bg'], '+3.2%', '↑ 优于上月'),
        ('最高得分', '92.3', C['green'], C['green_bg'], '北京分行', '↑ +2.1'),
        ('最低得分', '72.1', C['red'], C['red_bg'], '拉萨分行', '↓ -1.5'),
        ('完成率', '94.2%', C['teal'], C['teal_bg'], '整体进度', '正常'),
        ('参与机构', '12', C['purple'], C['purple_bg'], '全部', '正常'),
    ]
    for i, (title, num, col, bg, sub, sub2) in enumerate(kpis):
        x = cx+20+i*224
        c += R(x, yc, 204, 100, 14, C['card'], 'none', shadow=True)
        c += R(x, yc, 4, 100, 2, col)
        c += T(x+20, yc+20, title, C['text_secondary'], 12)
        c += T(x+20, yc+52, num, col, 28, True)
        c += T(x+100, yc+60, sub2, C['green'] if sub2[0]=='↑' else C['text_light'], 11)
        c += T(x+20, yc+82, sub, C['text_light'], 11)
        c += f'<rect x="{x+150}" y="{yc+15}" width="38" height="38" rx="10" fill="{bg}"/>\n'
    yc2 = yc + 120
    # Left: Overall ranking
    c += R(cx+20, yc2, 400, 340, 16, C['card'], 'none', shadow=True)
    c += T(cx+40, yc2+28, '机构总得分排名', C['text'], 15, True)
    c += tag(cx+310, yc2+18, 'TOP 5', C['primary_bg'], C['primary'], 22)
    rank_data = [
        ('🥇', '北京分行', '北方区', 92.3, C['primary']),
        ('🥈', '上海分行', '华东区', 88.9, C['teal']),
        ('🥉', '深圳分行', '华南区', 85.6, C['orange']),
        ('', '杭州分行', '华东区', 82.1, C['text_secondary']),
        ('', '成都分行', '西南区', 79.3, C['text_secondary']),
        ('', '天津分行', '北方区', 76.8, C['text_secondary']),
    ]
    for i, (emoji, name, grp, score, col) in enumerate(rank_data):
        ry = yc2+60+i*44
        c += T(cx+40, ry+14, emoji, C['text'], 16)
        if i < 3:
            c += f'<rect x="{cx+70}" y="{ry}" width="{score*4}" height="30" rx="6" fill="{col}"/>\n'
            c += T(cx+80, ry+20, name, C['card'], 12, True)
        else:
            c += f'<rect x="{cx+70}" y="{ry}" width="{score*4}" height="30" rx="6" fill="{C["border"]}"/>\n'
            c += T(cx+80, ry+20, name, C['text_secondary'], 12)
        c += T(cx+380, ry+20, f'{score}', col, 13, True, 'end')
        c += T(cx+70, ry+20, grp, C['text_light'], 10)
    # Center: Radar chart
    c += R(cx+440, yc2, 380, 340, 16, C['card'], 'none', shadow=True)
    c += T(cx+460, yc2+28, '多维度分析', C['text'], 15, True)
    c += legend_item(cx+460, yc2+55, C['primary'], '北京分行')
    c += legend_item(cx+590, yc2+55, C['teal'], '上海分行')
    c += radar_chart(cx+630, yc2+195, 120, [
        [0.92,0.85,0.88,0.90,0.78,0.82],
        [0.88,0.82,0.85,0.86,0.75,0.80],
    ], [C['primary'], C['teal']])
    dim_labels = ['业务发展','风险控制','服务质量','合规管理','创新指标','基础管理']
    angles = [math.pi*2*i/6 - math.pi/2 for i in range(6)]
    for i, lbl in enumerate(dim_labels):
        angle = angles[i]
        lx = cx+630 + 145 * math.cos(angle)
        ly = yc2+195 + 145 * math.sin(angle)
        c += T(lx, ly, lbl, C['text_secondary'], 10, False, 'middle')
    # Right: Gauge charts
    c += R(cx+840, yc2, 320, 340, 16, C['card'], 'none', shadow=True)
    c += T(cx+860, yc2+28, '综合完成率', C['text'], 15, True)
    gauges = [
        (cx+940, yc2+100, 60, 0.942, C['green'], '综合'),
        (cx+1060, yc2+100, 60, 0.856, C['primary'], '业务'),
        (cx+940, yc2+220, 60, 0.923, C['teal'], '风控'),
        (cx+1060, yc2+220, 60, 0.881, C['orange'], '合规'),
    ]
    for gx, gy, gr, val, col, lbl in gauges:
        c += gauge(gx, gy, gr, val, col)
        c += T(gx, gy+gr+16, lbl, C['text_secondary'], 11, False, 'middle')
    # Bottom: Trend
    yb = yc2 + 360
    c += R(cx+20, yb, 1140, 260, 16, C['card'], 'none', shadow=True)
    c += T(cx+40, yb+28, '月度得分趋势', C['text'], 15, True)
    c += T(cx+700, yb+28, '2025年10月 - 2026年3月', C['text_light'], 12)
    c += legend_item(cx+40, yb+55, C['primary'], '北京分行')
    c += legend_item(cx+170, yb+55, C['teal'], '上海分行')
    c += legend_item(cx+300, yb+55, C['orange'], '深圳分行')
    trend_months = ['10月','11月','12月','1月','2月','3月']
    trend_vals = [[75,82,78,85,88,92],[70,75,80,82,84,88],[68,72,75,78,80,85]]
    trend_colors = [C['primary'],C['teal'],C['orange']]
    all_y = yb+75
    for ti, (vals, col) in enumerate(zip(trend_vals, trend_colors)):
        n = len(vals)
        xs = [cx+40+i*(1060/(n-1)) for i in range(n)]
        max_v = 100; min_v = 50
        ys = [all_y+200-(v-min_v)/(max_v-min_v)*200 for v in vals]
        pts = ' '.join([f'{x:.1f},{y:.1f}' for x,y in zip(xs,ys)])
        area_pts = f'{cx+40},{all_y+200} ' + pts + f' {xs[-1]:.1f},{all_y+200}'
        c += f'<polygon points="{area_pts}" fill="{col}" opacity="0.1"/>\n'
        c += f'<polyline points="{pts}" fill="none" stroke="{col}" stroke-width="2.5"/>\n'
        for xi, yi, v in zip(xs, ys, vals):
            c += f'<circle cx="{xi:.1f}" cy="{yi:.1f}" r="4" fill="{col}"/>\n'
    for i, lbl in enumerate(trend_months):
        c += T(cx+40+i*212, all_y+215, lbl, C['text_light'], 11, False, 'middle')
    c += footer()
    write('03_report', c)

# ============================================================
# PAGE 4: System List
# ============================================================
def page_system_list():
    W, H = 1200, 800
    c = svg(W, H)
    c += defs()
    c += f'<rect width="{W}" height="{H}" fill="{C["bg"]}"/>\n'
    # Header
    c += f'<rect width="{W}" height="64" fill="{C["navy"]}"/>\n'
    c += f'<rect x="24" y="18" width="36" height="28" rx="8" fill="{C["primary"]}"/>\n'
    c += T(72, 38, '绩效管理系统', C['card'], 16, True)
    c += T(700, 38, '首页仪表盘', C['text_light'], 13)
    c += T(820, 38, '考核体系管理', C['primary'], 13)
    c += T(940, 38, '绩效结果展示', C['text_light'], 13)
    c += f'<rect x="1060" y="14" width="36" height="36" rx="18" fill="{C["primary"]}"/>\n'
    c += T(1078, 36, '张', C['card'], 14, True)
    c += T(1012, 38, '管理员 张三', C['card'], 13)
    # Sidebar
    c += f'<rect width="220" height="{H-64}" y="64" fill="{C["sidebar"]}"/>\n'
    items = ['仪表盘', '考核体系', '月度监测', '绩效报表', '通知中心']
    for i, item in enumerate(items):
        y = 100 + i*56
        if i == 1:
            c += f'<rect x="12" y="{y}" width="196" height="44" rx="10" fill="{C["primary"]}" opacity="0.15"/>\n'
            c += f'<rect x="12" y="{y}" width="4" height="44" rx="2" fill="{C["primary"]}"/>\n'
        c += T(36, y+27, '◈◇◉▣◔'[i], C['primary'] if i==1 else C['text_light'], 16)
        c += T(60, y+27, item, C['primary'] if i==1 else C['card'], 14, i==1)
    cx = 260
    c += T(cx+20, 100, '考核体系管理', C['text'], 22, True)
    c += f'<rect x="{cx+20}" y="{cx and 120}" width="0" height="0"/>\n'
    # Stats
    stats = [('全部体系', '5', C['primary']), ('启用中', '3', C['green']), ('已禁用', '2', C['text_light'])]
    for i, (t, n, col) in enumerate(stats):
        x = cx+20+i*120; y = 115
        c += I(x, y, 105, 36, C['bg'], C['border'], 8)
        c += T(x+10, y+22, t, C['text_secondary'], 11)
        c += T(x+60, y+22, n, col, 13, True, 'middle')
    # Create button
    c += f'<rect x="{cx+780}" y="115" width="160" height="40" rx="10" fill="{C["primary"]}"/>\n'
    c += T(cx+860, 139, '+ 创建体系', C['card'], 14, True, 'middle')
    # Search
    c += I(cx+20, 170, 300, 40, C['card'], C['border'], 10)
    c += T(cx+36, 194, '🔍  搜索考核体系...', C['text_light'], 13)
    # Table
    y = 230
    cols = [('体系名称', 280), ('描述', 300), ('机构数', 80), ('指标数', 80), ('状态', 100), ('创建时间', 140), ('操作', 120)]
    total_w = sum(cw for _,cw in cols)
    c += R(cx+20, y, total_w, 44, 12, C['card'], 'none')
    xx = cx+20
    for h, cw in cols:
        c += T(xx+10, y+27, h, C['text_secondary'], 11, True)
        xx += cw
    y += 44
    rows = [
        ('2026年度分行绩效考核体系', '适用于全行12家分行年度考核', '12', '45', '启用', '2026-01-15', True),
        ('2026年Q1专项考核', 'Q1专项业务考核，聚焦重点业务', '8', '20', '启用', '2026-01-10', True),
        ('2025年度考核体系', '历史年度考核数据存档', '12', '42', '禁用', '2025-01-01', False),
        ('2025年Q4考核', 'Q4专项业务考核', '10', '18', '禁用', '2025-10-01', False),
        ('2025年Q3考核', 'Q3专项业务考核', '10', '18', '禁用', '2025-07-01', False),
    ]
    for ri, (name, desc, inst, ind, status, date, enabled) in enumerate(rows):
        ry = y + ri*56
        c += R(cx+20, ry, total_w, 56, 0, C['card'] if ri%2==0 else C['card_hover'], C['border'], 0.5)
        xx = cx+20
        c += T(xx+10, ry+32, name, C['primary'] if ri==0 else C['text'], 12, ri==0)
        xx += 280
        c += T(xx+10, ry+32, desc, C['text_secondary'], 12)
        xx += 300
        c += T(xx+10, ry+32, inst, C['text'], 12, True, 'middle')
        xx += 80
        c += T(xx+10, ry+32, ind, C['text'], 12, True, 'middle')
        xx += 80
        tc = C['green'] if enabled else C['text_light']
        tbg = C['green_bg'] if enabled else C['bg']
        c += tag(xx+10, ry+16, status, tbg, tc, 22)
        xx += 100
        c += T(xx+10, ry+32, date, C['text_secondary'], 12)
        xx += 140
        c += T(xx+10, ry+32, '编辑', C['primary'], 12)
        c += T(xx+55, ry+32, '查看', C['text_secondary'], 12)
        c += T(xx+100, ry+32, '删除', C['red'], 12)
    # Pagination
    yp = y + 5*56 + 20
    c += T(cx+20, yp+10, '共 5 条', C['text_secondary'], 12)
    c += f'<rect x="{cx+600}" y="{yp-4}" width="36" height="32" rx="8" fill="{C["primary"]}"/>\n'
    c += T(cx+618, yp+16, '1', C['card'], 12, True, 'middle')
    c += I(cx+644, yp-4, 36, 32, C['card'], C['border'], 8)
    c += T(cx+662, yp+16, '2', C['text_secondary'], 12, True, 'middle')
    c += T(cx+690, yp+16, '›', C['text_secondary'], 16, False, 'middle')
    c += footer()
    write('04_system_list', c)

# ============================================================
# PAGE 5: Monitoring Detail
# ============================================================
def page_monitoring_detail():
    W, H = 1200, 800
    c = svg(W, H)
    c += defs()
    c += f'<rect width="{W}" height="{H}" fill="{C["bg"]}"/>\n'
    c += f'<rect width="{W}" height="64" fill="{C["navy"]}"/>\n'
    c += f'<rect x="24" y="18" width="36" height="28" rx="8" fill="{C["primary"]}"/>\n'
    c += T(72, 38, '绩效管理系统', C['card'], 16, True)
    c += T(700, 38, '月度考核监测', C['primary'], 13)
    c += f'<rect x="1060" y="14" width="36" height="36" rx="18" fill="{C["primary"]}"/>\n'
    c += T(1078, 36, '张', C['card'], 14, True)
    c += T(1012, 38, '管理员 张三', C['card'], 13)
    # Sidebar
    c += f'<rect width="220" height="{H-64}" y="64" fill="{C["sidebar"]}"/>\n'
    items = ['仪表盘', '考核体系', '月度监测', '绩效报表', '通知中心']
    for i, item in enumerate(items):
        y = 100 + i*56
        if i == 2:
            c += f'<rect x="12" y="{y}" width="196" height="44" rx="10" fill="{C["primary"]}" opacity="0.15"/>\n'
            c += f'<rect x="12" y="{y}" width="4" height="44" rx="2" fill="{C["primary"]}"/>\n'
        c += T(36, y+27, '◈◇◉▣◔'[i], C['primary'] if i==2 else C['text_light'], 16)
        c += T(60, y+27, item, C['primary'] if i==2 else C['card'], 14, i==2)
    cx = 260
    c += T(cx+20, 100, '← 返回', C['primary'], 13)
    c += T(cx+80, 100, '月度监测详情', C['text'], 22, True)
    c += T(cx+880, 100, '2026年3月', C['text_light'], 13, False, 'end')
    # Steps
    steps = [
        ('1\n发起', True, C['primary']),
        ('2\n收数中', True, C['teal']),
        ('3\n数据落库', True, C['teal']),
        ('4\n待确认', False, C['text_light']),
        ('5\n已发布', False, C['text_light']),
    ]
    sx = cx+20
    for i, (label, done, col) in enumerate(steps):
        sy = 130
        if done:
            c += f'<rect x="{sx}" y="{sy}" width="180" height="60" rx="12" fill="{col}"/>\n'
        else:
            c += f'<rect x="{sx}" y="{sy}" width="180" height="60" rx="12" fill="{C["card"]}" stroke="{C["border"]}"/>\n'
        lines = label.split('\n')
        for li, ln in enumerate(lines):
            c += T(sx+90, sy+24+li*18, ln, C['card'] if done else C['text_light'], 13, True, 'middle')
        if i < 4:
            c += T(sx+188, sy+30, '›', C['border'] if not done else C['card'], 20, False, 'middle')
        sx += 200
    # Info cards
    yi = 215
    info = [
        ('进行中监测', '3', C['primary']), ('待确认', '5', C['orange']),
        ('已确认', '7', C['green']), ('已截止', '2026-03-27', C['text_secondary']),
    ]
    for i, (t, v, col) in enumerate(info):
        x = cx+20+i*285; y = yi
        c += R(x, y, 265, 80, 14, C['card'], 'none', shadow=True)
        c += T(x+20, y+24, t, C['text_secondary'], 12)
        c += T(x+20, y+56, v, col, 24, True)
    # Tabs
    tabs = ['收数任务', '落库进度', '确认情况', '关联文件']
    for i, tab in enumerate(tabs):
        tx = cx+20+i*140; ty = yi+100
        if i == 0:
            c += f'<rect x="{tx}" y="{ty}" width="120" height="36" rx="10" fill="{C["primary"]}"/>\n'
            c += T(tx+60, ty+22, tab, C['card'], 13, True, 'middle')
        else:
            c += T(tx+60, ty+22, tab, C['text_secondary'], 13, False, 'middle')
    # Task table
    yt = yi+160
    cols = [('机构', 150), ('指标', 200), ('收数人', 130), ('进度目标', 100), ('实际值', 100), ('状态', 100), ('提交时间', 120)]
    tw = sum(cw for _,cw in cols)
    c += R(cx+20, yt, tw, 40, 12, C['card'], 'none')
    xx = cx+20
    for h, cw in cols:
        c += T(xx+8, yt+26, h, C['text_secondary'], 11, True)
        xx += cw
    yt += 40
    data = [
        ('北京分行', '日均存款余额', '王五/EMP005', '8.33', '7.85', '已提交', '03-27 10:30'),
        ('北京分行', '存款增长率', '王五/EMP005', '1.25%', '1.10%', '已提交', '03-27 10:30'),
        ('上海分行', '不良贷款率', '李四/EMP002', '0.125%', '0.110%', '待填写', '-'),
        ('深圳分行', '拨备覆盖率', '赵六/EMP003', '15.00%', '16.50%', '已提交', '03-27 11:00'),
        ('杭州分行', '日均存款余额', '孙七/EMP006', '8.33', '-', '待填写', '-'),
    ]
    for ri, row in enumerate(data):
        ry = yt + ri*48
        bg = C['card'] if ri%2==0 else C['card_hover']
        c += R(cx+20, ry, tw, 48, 0, bg, C['border'], 0.5)
        xx = cx+20
        for vi, (v, cw) in enumerate(zip(row, [c[1] for c in cols])):
            if vi == 5:
                tc = C['green'] if v=='已提交' else C['text_light']
                tbg = C['green_bg'] if v=='已提交' else C['bg']
                c += tag(xx+8, ry+12, v, tbg, tc, 22)
            else:
                c += T(xx+8, ry+28, v, C['text_secondary'] if vi==6 else C['text'], 11)
            xx += cw
    # Action buttons
    c += f'<rect x="{cx+20+tw-360}" y="{yt+5}" width="120" height="36" rx="10" fill="{C["primary"]}"/>\n'
    c += T(cx+20+tw-280, yt+27, '整体上传', C['card'], 13, True, 'middle')
    c += f'<rect x="{cx+20+tw-225}" y="{yt+5}" width="120" height="36" rx="10" fill="transparent" stroke="{C["border"]}"/>\n'
    c += T(cx+20+tw-165, yt+27, '终止收数', C['text_secondary'], 13, True, 'middle')
    c += footer()
    write('05_monitoring_detail', c)

# ============================================================
# PAGE 6: Collect Page
# ============================================================
def page_collect():
    W, H = 1200, 800
    c = svg(W, H)
    c += defs()
    c += f'<rect width="{W}" height="{H}" fill="{C["bg"]}"/>\n'
    c += f'<rect width="{W}" height="64" fill="{C["navy"]}"/>\n'
    c += f'<rect x="24" y="18" width="36" height="28" rx="8" fill="{C["primary"]}"/>\n'
    c += T(72, 38, '绩效管理系统', C['card'], 16, True)
    c += T(700, 38, '数据填写', C['card'], 13)
    c += T(820, 38, '绩效报表', C['text_light'], 13)
    c += f'<rect x="940" y="14" width="36" height="36" rx="18" fill="{C["teal"]}"/>\n'
    c += T(955, 36, '王', C['card'], 14, True)
    c += T(990, 38, '收数人 王五', C['card'], 13)
    # Sidebar
    c += f'<rect width="220" height="{H-64}" y="64" fill="{C["sidebar"]}"/>\n'
    items = ['仪表盘', '数据填写', '绩效报表', '通知中心']
    icons = ['◈', '◉', '▣', '◔']
    for i, (icon, item) in enumerate(zip(icons, items)):
        y = 100 + i*56
        if i == 1:
            c += f'<rect x="12" y="{y}" width="196" height="44" rx="10" fill="{C["teal"]}" opacity="0.15"/>\n'
            c += f'<rect x="12" y="{y}" width="4" height="44" rx="2" fill="{C["teal"]}"/>\n'
        c += T(36, y+27, icon, C['teal'] if i==1 else C['text_light'], 16)
        c += T(60, y+27, item, C['teal'] if i==1 else C['card'], 14, i==1)
    cx = 260
    c += T(cx+20, 100, '数据填写', C['text'], 22, True)
    c += T(cx+790, 100, '2026年3月  ·  2026年度分行绩效考核体系', C['text_light'], 13, False, 'end')
    # Progress
    c += I(cx+20, 130, 1140, 6, C['border'], C['border'], 3)
    c += I(cx+20, 130, 680, 6, C['teal'], C['teal'], 3)
    c += T(cx+20, 150, '收数进度：62%', C['text_secondary'], 12)
    c += T(cx+860, 150, '已填写 8/13 项  ·  截止 03-27 18:00', C['orange'], 12)
    # Institution cards
    institutions = [
        {
            'name': '北京分行',
            'deadline': '截止 03月27日 18:00',
            'tasks': [
                ('日均存款余额', '亿元', '8.33', '7.85', True),
                ('存款增长率', '%', '1.25', '1.10', True),
                ('不良贷款率', '%', '0.125', '', False),
            ]
        },
        {
            'name': '杭州分行',
            'deadline': '截止 03月27日 18:00',
            'tasks': [
                ('日均存款余额', '亿元', '8.33', '', False),
                ('存款增长率', '%', '1.25', '', False),
            ]
        },
    ]
    y = 170
    for inst in institutions:
        # Card
        c += R(cx+20, y, 1140, 80, 14, C['card'], 'none', shadow=True)
        c += R(cx+20, y, 4, 80, 2, C['teal'])
        c += T(cx+40, y+24, inst['name'], C['text'], 16, True)
        c += T(cx+40, y+52, inst['deadline'], C['orange'], 12)
        c += T(cx+200, y+24, '机构', C['text_light'], 11)
        c += T(cx+200, y+52, '北方区', C['text_light'], 11)
        c += T(cx+950, y+30, '↓ 下载Excel', C['primary'], 13)
        c += T(cx+1050, y+30, '↑ 上传Excel', C['teal'], 13)
        y += 90
        # Task rows
        c += R(cx+20, y, 1140, 44, 0, C['card'], 'none')
        headers = ['指标名称', '单位', '进度目标', '实际值']
        cws = [280, 100, 150, 150, 360]
        for hi, (h, cw) in enumerate(zip(headers, cws)):
            c += T(cx+30+sum(cws[:hi]), y+28, h, C['text_secondary'], 11, True)
        y += 44
        for ti, task in enumerate(inst['tasks']):
            name, unit, target, val, filled = task
            bg = C['card'] if ti%2==0 else C['card_hover']
            c += R(cx+20, y, 1140, 48, 0, bg, C['border'], 0.5)
            c += T(cx+30, y+28, name, C['text'], 12)
            c += T(cx+310, y+28, unit, C['text_secondary'], 12)
            c += T(cx+410, y+28, target, C['text'], 12, True, 'middle')
            # Value input
            c += I(cx+560, y+8, 140, 32, C['bg'], C['primary'] if filled else C['border'], 8)
            c += T(cx+580, y+27, val if val else '填写实际值', C['text_light'] if not filled else C['text'], 12)
            # Status
            if filled:
                c += tag(cx+720, y+12, '✓ 已填写', C['green_bg'], C['green'], 22)
            else:
                c += tag(cx+720, y+12, '待填写', C['orange_bg'], C['orange'], 22)
            y += 48
        # Submit button
        all_filled = all(t[3] for t in inst['tasks'])
        bc = C['teal'] if all_filled else C['border']
        c += f'<rect x="{cx+20+980}" y="{y-8}" width="140" height="40" rx="10" fill="{bc}"/>\n'
        c += T(cx+20+1050, y+12, '提交数据', C['card'] if all_filled else C['text_light'], 13, True, 'middle')
        y += 40
    c += footer()
    write('06_collect', c)

# ============================================================
# PAGE 7: Notifications
# ============================================================
def page_notifications():
    W, H = 1200, 800
    c = svg(W, H)
    c += defs()
    c += f'<rect width="{W}" height="{H}" fill="{C["bg"]}"/>\n'
    c += f'<rect width="{W}" height="64" fill="{C["navy"]}"/>\n'
    c += f'<rect x="24" y="18" width="36" height="28" rx="8" fill="{C["primary"]}"/>\n'
    c += T(72, 38, '绩效管理系统', C['card'], 16, True)
    c += T(700, 38, '月度考核监测', C['text_light'], 13)
    c += T(820, 38, '绩效结果展示', C['text_light'], 13)
    c += T(940, 38, '通知中心', C['primary'], 13)
    c += f'<rect x="1040" y="14" width="36" height="36" rx="18" fill="{C["primary"]}"/>\n'
    c += T(1058, 36, '张', C['card'], 14, True)
    c += T(990, 38, '管理员 张三', C['card'], 13)
    c += f'<rect x="940" y="14" width="28" height="28" rx="14" fill="{C["red"]}"/>\n'
    c += T(954, 32, '5', C['card'], 11, True, 'middle')
    # Sidebar
    c += f'<rect width="220" height="{H-64}" y="64" fill="{C["sidebar"]}"/>\n'
    items = ['仪表盘', '考核体系', '月度监测', '绩效报表', '通知中心']
    for i, item in enumerate(items):
        y = 100 + i*56
        if i == 4:
            c += f'<rect x="12" y="{y}" width="196" height="44" rx="10" fill="{C["primary"]}" opacity="0.15"/>\n'
            c += f'<rect x="12" y="{y}" width="4" height="44" rx="2" fill="{C["primary"]}"/>\n'
        c += T(36, y+27, '◈◇◉▣◔'[i], C['primary'] if i==4 else C['text_light'], 16)
        c += T(60, y+27, item, C['primary'] if i==4 else C['card'], 14, i==4)
    cx = 260
    c += T(cx+20, 100, '通知中心', C['text'], 22, True)
    c += T(cx+880, 100, '共 15 条  ·  5 条未读', C['text_secondary'], 13, False, 'end')
    # Tabs
    for i, (tab, x) in enumerate([('全部', cx+20), ('未读', cx+120), ('已读', cx+200)]):
        if i == 1:
            c += f'<rect x="{x}" y="120" width="80" height="36" rx="10" fill="{C["primary"]}"/>\n'
            c += T(x+40, 142, tab, C['card'], 13, True, 'middle')
            c += f'<rect x="{x+70}" y="122" width="18" height="18" rx="9" fill="{C["red"]}"/>\n'
            c += T(x+79, 135, '5', C['card'], 10, True, 'middle')
        else:
            c += T(x+40, 142, tab, C['text_secondary'], 13, False, 'middle')
    c += T(cx+880, 142, '全部标为已读', C['primary'], 13)
    # Notification cards
    notifs = [
        ('📊', '【收数任务】您有新的数据待填写', '体系：2026年度分行绩效考核体系，月份：2026年3月，请于03月27日18:00前完成。', '3分钟前', True, C['primary_bg'], C['primary']),
        ('🔔', '【截止提醒】收数即将截止', '您负责的北京分行存款数据还未提交，距离截止还有30分钟，请尽快填写。', '28分钟前', True, C['orange_bg'], C['orange']),
        ('⏰', '【进度通知】数据落库完成', '2026年2月绩效数据已全部落库完成，请通知相关机构负责人确认。', '2小时前', False, C['bg'], C['text_secondary']),
        ('✅', '【确认通知】请确认本机构绩效数据', '2026年2月绩效数据已生成，请确认北京分行的绩效数据，确认后管理员将发布。', '5小时前', False, C['bg'], C['text_secondary']),
        ('📢', '【发布通知】2026年2月绩效结果已发布', '管理员已发布2026年2月的绩效结果，数据现已公开，请各机构负责人查看。', '1天前', False, C['bg'], C['text_secondary']),
    ]
    y = 175
    for emoji, title, content, time, unread, bg, fg in notifs:
        c += R(cx+20, y, 1140, 100, 14, C['card'], 'none', shadow=True)
        if unread:
            c += R(cx+20, y, 5, 100, 2, fg)
        c += T(cx+40, y+22, emoji, C['text'], 24)
        c += T(cx+80, y+22, title, fg, 14, True)
        c += T(cx+900, y+22, time, C['text_light'], 12, False, 'end')
        c += T(cx+80, y+52, content, C['text_secondary'], 12)
        if unread:
            c += T(cx+80, y+78, '标为已读  ·  查看详情 →', fg, 12)
        else:
            c += T(cx+80, y+78, '查看详情 →', C['text_light'], 12)
        y += 115
    # Pagination
    c += T(cx+20, y+10, '共 15 条', C['text_secondary'], 12)
    for i in range(2):
        px = cx+600+i*44
        if i == 0:
            c += f'<rect x="{px}" y="{y-4}" width="36" height="32" rx="8" fill="{C["primary"]}"/>\n'
            c += T(px+18, y+16, '1', C['card'], 12, True, 'middle')
        else:
            c += I(px, y-4, 36, 32, C['card'], C['border'], 8)
            c += T(px+18, y+16, '2', C['text_secondary'], 12, True, 'middle')
    c += T(cx+700, y+16, '›', C['text_secondary'], 18, False, 'middle')
    c += footer()
    write('07_notifications', c)

# ============================================================
# PAGE 8: Overview - Richest Report Page
# ============================================================
def page_overview():
    W, H = 1200, 950
    c = svg(W, H)
    c += defs()
    c += f'<rect width="{W}" height="{H}" fill="{C["bg"]}"/>\n'
    c += f'<rect width="{W}" height="64" fill="{C["navy"]}"/>\n'
    c += f'<rect x="24" y="18" width="36" height="28" rx="8" fill="{C["primary"]}"/>\n'
    c += T(72, 38, '绩效管理系统', C['card'], 16, True)
    c += T(700, 38, '月度考核监测', C['text_light'], 13)
    c += T(820, 38, '绩效结果展示', C['primary'], 13)
    c += f'<rect x="1060" y="14" width="36" height="36" rx="18" fill="{C["primary"]}"/>\n'
    c += T(1078, 36, '张', C['card'], 14, True)
    c += T(1012, 38, '管理员 张三', C['card'], 13)
    # Sidebar
    c += f'<rect width="220" height="{H-64}" y="64" fill="{C["sidebar"]}"/>\n'
    items = ['仪表盘', '考核体系', '月度监测', '绩效报表', '通知中心']
    for i, item in enumerate(items):
        y = 100 + i*56
        if i == 3:
            c += f'<rect x="12" y="{y}" width="196" height="44" rx="10" fill="{C["primary"]}" opacity="0.15"/>\n'
            c += f'<rect x="12" y="{y}" width="4" height="44" rx="2" fill="{C["primary"]}"/>\n'
        c += T(36, y+27, '◈◇◉▣◔'[i], C['primary'] if i==3 else C['text_light'], 16)
        c += T(60, y+27, item, C['primary'] if i==3 else C['card'], 14, i==3)
    cx = 260
    c += T(cx+20, 100, '绩效总览', C['text'], 22, True)
    c += T(cx+790, 100, '2026年3月  ·  2026年度分行绩效考核体系  ·  12家机构', C['text_light'], 13, False, 'end')
    # Theme switcher
    c += R(cx+20, 125, 240, 40, 10, C['card'], C['border'], 1)
    c += T(cx+36, 147, '🎨 主题配色', C['text'], 12)
    themes = [('默认靛蓝', C['primary']), ('活力青绿', C['teal']), ('商务蓝', '#2563EB'), ('活力红', '#DC2626')]
    for i, (t, col) in enumerate(themes):
        tx = cx+140+i*60
        c += f'<rect x="{tx}" y="{cx and 133}" width="18" height="18" rx="9" fill="{col}"/>\n'
        if i == 0:
            c += f'<circle cx="{tx+9}" cy="{cx and 142}" r="10" fill="none" stroke="{col}" stroke-width="2"/>\n'
    # Filters
    for i, (fval, fx) in enumerate([('2026年度分行绩效考核体系', cx+20), ('2026年', cx+280), ('3月', cx+400), ('全部机构', cx+500), ('全部分组', cx+640)]):
        c += I(fx, 130, 140, 36, C['card'], C['border'], 8)
        c += T(fx+12, 152, fval, C['text_secondary'], 12)
        c += T(fx+116, 152, '▼', C['text_light'], 10, False, 'middle')
    # Tab pills
    tabs = [('总览分析', True), ('趋势分析', False), ('维度分析', False), ('分组对比', False), ('指标完成度', False)]
    for i, (tab, active) in enumerate(tabs):
        tx = cx+20+i*120
        if active:
            c += f'<rect x="{tx}" y="186" width="110" height="36" rx="10" fill="{C["primary"]}"/>\n'
            c += T(tx+55, 208, tab, C['card'], 13, True, 'middle')
        else:
            c += T(tx+55, 208, tab, C['text_secondary'], 13, False, 'middle')
    y = 245
    # KPI row
    kpis = [
        ('机构平均分', '84.6', '+2.3', C['primary'], C['primary_bg'], 'vs上月'),
        ('最高得分', '92.3', '北京', C['green'], C['green_bg'], '领先平均 +7.7'),
        ('最低得分', '72.1', '拉萨', C['red'], C['red_bg'], '低于平均 -12.5'),
        ('完成率', '94.2%', '+1.8%', C['teal'], C['teal_bg'], 'vs上月'),
        ('参与机构', '12', '全部', C['purple'], C['purple_bg'], '较上月+0'),
    ]
    for i, (title, num, badge, col, bg, sub) in enumerate(kpis):
        x = cx+20+i*224
        c += R(x, y, 204, 100, 14, C['card'], 'none', shadow=True)
        c += R(x, y, 4, 100, 2, col)
        c += T(x+20, y+20, title, C['text_secondary'], 12)
        c += T(x+20, y+56, num, col, 30, True)
        c += f'<rect x="{x+90}" y="{y+52}" width="55" height="22" rx="6" fill="{bg}"/>\n'
        c += T(x+117, y+66, badge, col, 11, True, 'middle')
        c += T(x+20, y+82, sub, C['text_light'], 10)
        c += f'<rect x="{x+150}" y="{y+15}" width="38" height="38" rx="10" fill="{bg}"/>\n'
    y2 = y + 120
    # Left: Ranking
    c += R(cx+20, y2, 380, 360, 16, C['card'], 'none', shadow=True)
    c += T(cx+40, y2+28, '机构得分排名', C['text'], 15, True)
    c += T(cx+300, y2+28, '北方区 ▼', C['text_light'], 12, False, 'end')
    rank = [
        ('🥇', '北京分行', '北方区', 92.3, C['primary'], C['primary_bg'], True),
        ('🥈', '上海分行', '华东区', 88.9, C['teal'], C['teal_bg'], True),
        ('🥉', '深圳分行', '华南区', 85.6, C['orange'], C['orange_bg'], True),
        ('', '杭州分行', '华东区', 82.1, C['text_secondary'], C['bg'], False),
        ('', '成都分行', '西南区', 79.3, C['text_secondary'], C['bg'], False),
        ('', '天津分行', '北方区', 76.8, C['text_secondary'], C['bg'], False),
        ('', '武汉分行', '华中区', 74.2, C['text_secondary'], C['bg'], False),
        ('', '西安分行', '西北区', 72.1, C['text_secondary'], C['bg'], False),
    ]
    for i, (emoji, name, grp, score, col, bg, top) in enumerate(rank):
        ry = y2+60+i*36
        c += T(cx+40, ry+12, emoji, C['text'], 14)
        if top:
            c += f'<rect x="{cx+70}" y="{ry}" width="{score*3.6}" height="28" rx="6" fill="{col}"/>\n'
            c += T(cx+80, ry+18, name, C['card'], 12, True)
        else:
            c += f'<rect x="{cx+70}" y="{ry}" width="{score*3.6}" height="28" rx="6" fill="{C["border"]}"/>\n'
            c += T(cx+80, ry+18, name, C['text_secondary'], 12)
        c += T(cx+380, ry+18, f'{score}', col, 13, True, 'end')
        c += T(cx+70, ry+18, grp, C['text_light'], 9)
    # Center: Radar
    c += R(cx+420, y2, 380, 360, 16, C['card'], 'none', shadow=True)
    c += T(cx+440, y2+28, '维度得分分析', C['text'], 15, True)
    c += legend_item(cx+440, y2+55, C['primary'], '北京分行')
    c += legend_item(cx+570, y2+55, C['teal'], '上海分行')
    c += legend_item(cx+700, y2+55, C['orange'], '深圳分行')
    c += radar_chart(cx+610, y2+195, 120, [
        [0.92,0.85,0.88,0.90,0.78,0.82],
        [0.88,0.82,0.85,0.86,0.75,0.80],
        [0.85,0.80,0.82,0.84,0.72,0.78],
    ], [C['primary'], C['teal'], C['orange']])
    dims = ['业务发展','风险控制','服务质量','合规管理','创新指标','基础管理']
    for i, lbl in enumerate(dims):
        angle = math.pi*2*i/6 - math.pi/2
        lx = cx+610 + 148 * math.cos(angle)
        ly = y2+195 + 148 * math.sin(angle)
        c += T(lx, ly, lbl, C['text_secondary'], 10, False, 'middle')
    # Right: Gauges
    c += R(cx+820, y2, 340, 360, 16, C['card'], 'none', shadow=True)
    c += T(cx+840, y2+28, '综合完成率', C['text'], 15, True)
    gauges_data = [
        (cx+900, y2+100, 70, 0.942, C['green'], '综合完成率'),
        (cx+1040, y2+100, 70, 0.856, C['primary'], '业务发展'),
        (cx+900, y2+230, 70, 0.923, C['teal'], '风险控制'),
        (cx+1040, y2+230, 70, 0.881, C['orange'], '合规管理'),
    ]
    for gx, gy, gr, val, col, lbl in gauges_data:
        c += gauge(gx, gy, gr, val, col)
        c += T(gx, gy+gr+18, lbl, C['text_secondary'], 11, False, 'middle')
    y3 = y2 + 380
    # Trend row
    c += R(cx+20, y3, 1140, 270, 16, C['card'], 'none', shadow=True)
    c += T(cx+40, y3+28, '月度得分趋势（近6月）', C['text'], 15, True)
    c += legend_item(cx+40, y3+55, C['primary'], '北京分行')
    c += legend_item(cx+170, y3+55, C['teal'], '上海分行')
    c += legend_item(cx+300, y3+55, C['orange'], '深圳分行')
    c += legend_item(cx+430, y3+55, C['purple'], '杭州分行')
    trend = [
        ([72,78,75,82,88,92], C['primary']),
        ([68,72,75,80,84,88], C['teal']),
        ([65,70,72,78,80,85], C['orange']),
        ([62,68,70,75,78,82], C['purple']),
    ]
    ty = y3+80
    for vals, col in trend:
        xs = [cx+40+i*190 for i in range(len(vals))]
        ys = [ty+185-v*1.85 for v in vals]
        pts = ' '.join([f'{x:.1f},{y:.1f}' for x,y in zip(xs,ys)])
        area = f'{cx+40},{ty+185} ' + pts + f' {xs[-1]:.1f},{ty+185}'
        c += f'<polygon points="{area}" fill="{col}" opacity="0.08"/>\n'
        c += f'<polyline points="{pts}" fill="none" stroke="{col}" stroke-width="2.5" stroke-linecap="round"/>\n'
        for x, y in zip(xs, ys):
            c += f'<circle cx="{x:.1f}" cy="{y:.1f}" r="3.5" fill="{col}"/>\n'
    for i, lbl in enumerate(['10月','11月','12月','1月','2月','3月']):
        c += T(cx+40+i*190, ty+200, lbl, C['text_light'], 11, False, 'middle')
    c += footer()
    write('08_overview', c)

def write(name, content):
    path = f'{OUT_DIR}/{name}.svg'
    with open(path, 'w') as f:
        f.write(content)
    print(f'{name}.svg done ({len(content)} bytes)')

def footer():
    return '</svg>\n'

# Run all
page_login()
page_dashboard()
page_report()
page_system_list()
page_monitoring_detail()
page_collect()
page_notifications()
page_overview()
print(f'\nAll modern UI mockups saved to {OUT_DIR}')
