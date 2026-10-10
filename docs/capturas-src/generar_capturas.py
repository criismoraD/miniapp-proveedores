"""
Genera maquetas HTML fieles a los layouts de Android (valores de colors.xml,
dimensiones dp de los XML, iconos vectoriales, fuentes y datos reales de
DataRepository.java). Sirve como vista previa; el render final en un
dispositivo puede variar ligeramente (fuente sans, sombras, ripple).

Uso (desde la raíz del repo):
    python3 docs/capturas-src/generar_capturas.py
    node docs/capturas-src/capturar.mjs
"""
import html
import math
import os
import re
import xml.etree.ElementTree as ET

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "app", "src", "main", "res")
OUT_HTML = os.environ.get("CAPTURAS_HTML", "/tmp/capturas_html")
DATA_SRC = os.path.join(ROOT, "app", "src", "main", "java", "com", "example",
                        "provedoreschiclayo", "data", "DataRepository.java")


# ---------------------------------------------------------------- recursos
def load_colors(path):
    colors = {}
    for el in ET.parse(path).getroot():
        if el.tag == "color":
            colors[el.get("name")] = el.text.strip()
    return colors


COLORS = load_colors(os.path.join(RES, "values", "colors.xml"))

# ---------------------------------------------------------------- temas
# CAPTURAS_TEMA=papel (por defecto, colors.xml tal cual) | comercial (variante de catálogo).
THEME = os.environ.get("CAPTURAS_TEMA", "papel")
PALETA_COMERCIAL = {
    "paper_bg": "#F4F6FB", "paper_surface": "#FFFFFF", "paper_deep": "#EEF2F8",
    "paper_on_ink": "#FFFFFF", "ink": "#0F172A", "ink_soft": "#334155", "ink_muted": "#64748B",
    "rule": "#E2E8F0", "rule_strong": "#CBD5E1",
    "gilt": "#F97316", "gilt_soft": "#FFF4EC", "gilt_dark": "#C2410C",
    "stamp": "#16A34A", "stamp_soft": "#DCFCE7", "sage": "#15803D", "sage_soft": "#DCFCE7",
    "scrim": "#8C0F172A",
}
if THEME == "comercial":
    COLORS.update(PALETA_COMERCIAL)


def c(name):
    v = COLORS[name]
    if len(v) == 9:  # #AARRGGBB -> rgba
        a = int(v[1:3], 16) / 255
        r, g, b = int(v[3:5], 16), int(v[5:7], 16), int(v[7:9], 16)
        return f"rgba({r},{g},{b},{a:.2f})"
    return "#" + v[-6:]


def icon(name, size=20, color="currentColor"):
    tree = ET.parse(os.path.join(RES, "drawable", name + ".xml")).getroot()
    d = tree.find("path").get("{http://schemas.android.com/apk/res/android}pathData")
    return (f'<svg width="{size}" height="{size}" viewBox="0 0 24 24" style="color:{color};flex:none">'
            f'<path fill="currentColor" d="{d}"/></svg>')


def fonts_css():
    base = "file://" + os.path.join(RES, "font").replace(os.sep, "/")
    return f"""
    @font-face {{ font-family: EBG; src: url('{base}/ebgaramond_regular.ttf'); font-weight: 400; }}
    @font-face {{ font-family: EBG; src: url('{base}/ebgaramond_semibold.ttf'); font-weight: 600; }}
    @font-face {{ font-family: EBG; src: url('{base}/ebgaramond_bold.ttf'); font-weight: 700; }}
    @font-face {{ font-family: EBG; src: url('{base}/ebgaramond_italic.ttf'); font-style: italic; }}
    """


def img_url(name):
    return "file://" + os.path.join(RES, "drawable", name).replace(os.sep, "/")


PAPER = "file://" + os.path.join(RES, "drawable-nodpi", "paper_texture.png").replace(os.sep, "/")


# ---------------------------------------------------------------- datos reales
import sys
sys.path.insert(0, os.path.dirname(__file__))


def sin_emoji(texto):
    # Chromium headless de este entorno no trae fuente de emoji; la app sí los muestra.
    return re.sub(r'^[^\w\u00C0-\u024F]+', '', texto).strip()

import datos  # noqa: E402  (extractor de DataRepository.java)


def money(v):
    return f"S/ {v:.2f}"


# ---------------------------------------------------------------- CSS base
CSS = f"""
{fonts_css()}
* {{ box-sizing: border-box; margin: 0; padding: 0; }}
html, body {{ background: #444; }}
.phone {{
  width: 360px; height: 760px; position: relative; overflow: hidden;
  background: {c('paper_bg')} url('{PAPER}') repeat;
  font-family: Roboto, "Helvetica Neue", Arial, sans-serif; color: {c('ink')};
}}
.statusbar {{ height: 26px; }}
.navbar {{ position: absolute; left: 0; right: 0; bottom: 0; height: 22px; background: {c('paper_bg')}; }}
.overline {{
  font: 500 11px/1.3 Roboto, Arial, sans-serif; letter-spacing: .14em; text-transform: uppercase;
  color: {c('ink_muted')};
}}
.row {{ display: flex; align-items: center; }}
.between {{ justify-content: space-between; }}
.center {{ text-align: center; }}
.mt {{ margin-top: 10px; }}
.masthead {{ font: 700 32px/1.1 EBG, Georgia, serif; text-align: center; color: {c('ink')}; margin-top: 4px; letter-spacing: .02em; }}
.rule2 {{ height: 2px; background: {c('ink')}; margin-top: 10px; }}
.rule1 {{ height: 1px; background: {c('ink')}; margin-top: 3px; }}
.hr {{ height: 1px; background: {c('rule')}; }}

.header {{ padding: 0 18px; }}
.textbtn {{ font: 500 13px Roboto, Arial, sans-serif; color: {c('ink')}; display: flex; align-items: center; gap: 2px; height: 36px; }}
.locrow {{ display: flex; gap: 10px; margin-top: 14px; align-items: center; }}
.locfield {{
  flex: 1; min-width: 0; display: flex; align-items: center; gap: 10px; padding: 9px 8px 9px 12px;
  background: {c('paper_surface')}; border: 1px solid {c('rule_strong')}; border-radius: 10px;
}}
.locname {{ font: 600 15px/1.25 EBG, Georgia, serif; color: {c('ink')}; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }}
.outlined {{
  height: 48px; padding: 0 14px; display: flex; align-items: center; gap: 6px;
  border: 1px solid {c('rule_strong')}; border-radius: 10px; background: transparent;
  font: 500 14px Roboto, Arial, sans-serif; color: {c('ink')};
}}
.search {{
  margin-top: 12px; height: 56px; display: flex; align-items: center; gap: 10px; padding: 0 14px;
  background: {c('paper_surface')}; border: 1px solid {c('rule_strong')}; border-radius: 12px;
  font: 400 15px Roboto, Arial, sans-serif; color: {c('ink_muted')};
}}
.chips {{ display: flex; gap: 8px; margin-top: 8px; overflow: hidden; white-space: nowrap; }}
.chip {{
  height: 32px; padding: 0 14px; border-radius: 8px; border: 1px solid {c('rule_strong')};
  display: inline-flex; align-items: center; gap: 6px; flex: none;
  font: 500 13px Roboto, Arial, sans-serif; color: {c('ink_soft')};
}}
.chip.on {{ background: {c('gilt_soft')}; border-color: transparent; color: {c('gilt_dark')}; }}
.sortlabel {{ margin-top: 10px; }}

.list {{ padding: 0 12px 110px; }}
.card {{
  background: {c('paper_surface')}; border: 1px solid {c('rule')}; border-radius: 6px; padding: 14px; margin-bottom: 12px;
  position: relative;
}}
.top {{ display: flex; align-items: center; }}
.plate {{
  width: 96px; height: 96px; border-radius: 4px; background: {c('paper_deep')};
  border: .5px solid {c('rule_strong')}; overflow: hidden; position: relative; flex: none;
}}
.plate img {{ width: 100%; height: 100%; object-fit: cover; filter: saturate(.84) sepia(.06); }}
.stamp {{
  position: absolute; left: 50%; bottom: 8px; transform: translateX(-50%) rotate(-10deg);
  border: 2px solid {c('stamp')}; border-radius: 4px; background: {c('stamp_soft')};
  color: {c('stamp')}; font: 700 10px/1 Roboto, Arial, sans-serif; letter-spacing: .12em; text-transform: uppercase;
  padding: 3px 6px; opacity: .92; white-space: nowrap;
}}
.pname {{ font: 600 20px/1.15 EBG, Georgia, serif; color: {c('ink')}; margin-top: 2px; }}
.pres {{ font: italic 400 14px/1.3 EBG, Georgia, serif; color: {c('ink_soft')}; margin-top: 1px; }}
.sup {{ display: flex; align-items: center; margin-top: 8px; gap: 6px; }}
.supname {{ flex: 1; font: 500 13px Roboto, Arial, sans-serif; color: {c('ink')}; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }}
.pill-rule {{ font: 500 11px Roboto, Arial, sans-serif; color: {c('ink_soft')}; background: {c('paper_deep')}; border-radius: 4px; padding: 2px 6px; white-space: nowrap; }}
.pill-sage {{ font: 500 12px Roboto, Arial, sans-serif; color: {c('sage')}; background: {c('sage_soft')}; border-radius: 4px; padding: 3px 7px; white-space: nowrap; }}
.pill-gilt {{ font: 700 10px Roboto, Arial, sans-serif; letter-spacing: .14em; text-transform: uppercase; color: {c('gilt_dark')}; background: {c('gilt_soft')}; border: 1px solid {c('gilt')}; border-radius: 4px; padding: 3px 7px; white-space: nowrap; }}
.price {{ font: 700 24px/1 EBG, Georgia, serif; color: {c('ink')}; font-variant-numeric: lining-nums tabular-nums; }}
.hr2 {{ height: 1px; background: {c('rule')}; margin: 12px 0 10px; }}
.btnrow {{ display: flex; gap: 8px; margin-top: 12px; }}
.btn-out {{ flex: 1; height: 42px; border: 1px solid {c('rule_strong')}; border-radius: 10px; display: flex; align-items: center; justify-content: center; gap: 6px; font: 500 13px Roboto, Arial, sans-serif; color: {c('ink')}; }}
.btn-fill {{ flex: 1; height: 42px; border-radius: 10px; background: {c('ink')}; color: {c('paper_on_ink')}; display: flex; align-items: center; justify-content: center; gap: 6px; font: 500 13px Roboto, Arial, sans-serif; }}
.savings {{ margin-top: 10px; padding: 6px 10px; background: {c('sage_soft')}; color: {c('sage')}; font: italic 400 13px/1.3 EBG, Georgia, serif; border-radius: 4px; }}

.cartbar {{
  position: absolute; left: 12px; right: 12px; bottom: 34px; border-radius: 16px; background: {c('ink')};
  padding: 12px 12px 12px 18px; display: flex; align-items: center; gap: 8px;
  box-shadow: 0 8px 18px rgba(30,35,49,.28);
}}
.cartbar .t {{ font: 600 17px/1.2 EBG, Georgia, serif; color: {c('paper_on_ink')}; }}
.cartbar .s {{ font: 400 12px/1.3 Roboto, Arial, sans-serif; color: {c('gilt')}; margin-top: 1px; }}
.cartbtn {{ height: 40px; padding: 0 14px; border-radius: 10px; background: {c('paper_surface')}; color: {c('ink')}; display: flex; align-items: center; gap: 4px; font: 500 14px Roboto, Arial, sans-serif; flex: none; }}

.scrim {{ position: absolute; inset: 0; background: {c('scrim')}; }}
.sheet {{
  position: absolute; left: 0; right: 0; bottom: 0; background: {c('paper_surface')};
  border-radius: 28px 28px 0 0; padding: 10px 20px 24px; box-shadow: 0 -6px 24px rgba(30,35,49,.22);
}}
.handle {{ width: 36px; height: 4px; border-radius: 2px; background: {c('rule_strong')}; margin: 0 auto; }}
.sheet .display {{ font: 700 24px/1.15 EBG, Georgia, serif; color: {c('ink')}; }}
.iconbtn {{ width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; color: {c('ink_muted')}; }}
.body {{ font: 400 14px/1.4 Roboto, Arial, sans-serif; color: {c('ink_soft')}; }}
.block {{ background: {c('paper_surface')}; border: 1px solid {c('rule')}; border-radius: 6px; padding: 14px; }}
.block.active {{ background: {c('gilt_soft')}; border: 1.25px solid {c('gilt')}; }}
.blockdeep {{ background: {c('paper_deep')}; border-radius: 8px; padding: 10px; }}
.stepper {{ display: flex; align-items: center; gap: 4px; }}
.stepbtn {{ width: 40px; height: 40px; border-radius: 10px; border: 1px solid {c('rule_strong')}; display: flex; align-items: center; justify-content: center; color: {c('ink')}; }}
.stepbtn.fill {{ background: {c('ink')}; border-color: transparent; color: {c('paper_on_ink')}; }}
.qty {{ width: 40px; text-align: center; font: 700 22px EBG, Georgia, serif; color: {c('ink')}; }}
.money {{ font: 600 16px Roboto, Arial, sans-serif; color: {c('ink')}; }}
.label {{ font: 500 10px Roboto, Arial, sans-serif; letter-spacing: .14em; text-transform: uppercase; color: {c('ink_muted')}; }}
"""


CSS_COMERCIAL = """
.phone { background: #F4F6FB !important; }
.overline { letter-spacing: .08em; font-weight: 600; }
.masthead { font: 800 28px/1.1 Roboto, Arial, sans-serif; color: #1D4ED8; letter-spacing: -.01em; margin-top: 2px; }
.rule2, .rule1 { display: none; }
.hr { background: #E2E8F0; }
.textbtn { font-weight: 600; color: #1D4ED8; }
.locfield, .search, .outlined { background: #FFFFFF; border: none; box-shadow: 0 1px 2px rgba(15,23,42,.06), 0 2px 8px rgba(15,23,42,.06); }
.locfield { border-radius: 14px; }
.search { border-radius: 28px; }
.outlined { border-radius: 14px; color: #1D4ED8; }
.locname { font-weight: 700; font-size: 15px; }
.chips { margin-top: 10px; }
.chip { border: none; border-radius: 999px; background: #FFFFFF; box-shadow: 0 1px 2px rgba(15,23,42,.08); color: #334155; }
.chip.on { background: #1D4ED8; color: #FFFFFF; box-shadow: 0 4px 10px rgba(29,78,216,.28); }
.card, .block { background: #FFFFFF; border: none; border-radius: 18px;
  box-shadow: 0 1px 2px rgba(15,23,42,.06), 0 6px 16px rgba(15,23,42,.08); }
.block.active { background: #EFF6FF; border: 1.5px solid #1D4ED8; box-shadow: 0 6px 16px rgba(29,78,216,.16); }
.plate { border: none; border-radius: 14px; background: #EEF2F8; }
.plate img { filter: none; }
.stamp { background: #16A34A; color: #FFFFFF; border: none; border-radius: 999px; transform: translateX(-50%) rotate(0deg); padding: 4px 9px; letter-spacing: .08em; }
.pname { font: 700 19px/1.2 Roboto, Arial, sans-serif; }
.pres { font: 400 13px/1.3 Roboto, Arial, sans-serif; color: #64748B; }
.price { font: 800 26px/1 Roboto, Arial, sans-serif; color: #0F172A; }
.pill-rule { border-radius: 999px; background: #EEF2F8; color: #334155; }
.pill-sage { border-radius: 999px; }
.pill-gilt { border: none; border-radius: 999px; background: #FFF4EC; color: #C2410C; letter-spacing: .06em; }
.btn-out { border: none; border-radius: 12px; background: #EEF2F8; color: #1D4ED8; font-weight: 600; }
.btn-fill { border-radius: 12px; background: #1D4ED8; box-shadow: 0 4px 12px rgba(29,78,216,.3); font-weight: 600; }
.savings { background: #DCFCE7; color: #15803D; font: 500 12.5px/1.35 Roboto, Arial, sans-serif; border-radius: 12px; }
.cartbar { background: #1D4ED8; border-radius: 20px; box-shadow: 0 10px 24px rgba(29,78,216,.35); }
.cartbar .t { font: 700 17px/1.2 Roboto, Arial, sans-serif; }
.cartbar .s { color: #BFDBFE; }
.cartbtn { border-radius: 12px; color: #1D4ED8; font-weight: 700; }
.sheet { border-radius: 28px 28px 0 0; box-shadow: 0 -8px 28px rgba(15,23,42,.18); }
.sheet .display { font: 800 24px/1.15 Roboto, Arial, sans-serif; letter-spacing: -.01em; }
.stepbtn { border: none; border-radius: 12px; background: #EEF2F8; }
.stepbtn.fill { background: #1D4ED8; color: #FFFFFF; box-shadow: 0 4px 10px rgba(29,78,216,.3); }
.qty { font: 800 22px Roboto, Arial, sans-serif; }
.blockdeep { border-radius: 14px; background: #EEF2F8; }
.handle { background: #CBD5E1; }
"""


def page(body, css_extra=""):
    out = f"""<!DOCTYPE html><html lang="es"><head><meta charset="utf-8">
<style>{CSS}{css_extra}</style></head><body>
<div class="phone">{body}</div></body></html>"""
    if THEME == "comercial":
        # Variante comercial: sans-serif en todo el documento (CSS base incluido) y capa de estilo propia.
        # La capa comercial va DESPUÉS del CSS base para poder sobrescribirlo.
        out = out.replace("</style>", CSS_COMERCIAL + "</style>", 1)
        out = re.sub(r"EBG,\s*Georgia,\s*serif", "Roboto, Arial, sans-serif", out)
    return out


def base_screen(cart_html="", products_html="", extra_top=""):
    return f"""
<div class="statusbar"></div>
<div class="header">
  <div class="row between" style="height:36px">
    <span class="overline">Viernes 9 de octubre</span>
    <span class="textbtn">Bodega {icon('ic_arrow_drop_down', 20, c('gilt'))}</span>
  </div>
  <div class="masthead">ProveeChiclayo</div>
  <div class="overline center" style="margin-top:2px">Red mayorista · Chiclayo y Lambayeque</div>
  <div class="rule2"></div><div class="rule1"></div>
  <div class="locrow">
    <div class="locfield">
      {icon('ic_place', 20, c('gilt'))}
      <div style="flex:1;min-width:0">
        <div class="overline">Mi bodega en</div>
        <div class="locname">{html.escape(datos.LOCATIONS[0]['name'])}</div>
      </div>
      {icon('ic_arrow_drop_down', 20, c('ink_muted'))}
    </div>
    <div class="outlined">{icon('ic_map', 18, c('ink'))} Mapa</div>
  </div>
  <div class="search">{icon('ic_search', 20, c('ink_muted'))}<span>Buscar sporade, arroz, aceite, leche…</span></div>
  <div class="chips">
    <span class="chip on">Todos</span><span class="chip">Bebidas</span><span class="chip">Abarrotes</span>
    <span class="chip">Golosinas</span><span class="chip">Limpieza</span>
  </div>
  <div class="overline sortlabel">Comparar proveedores por</div>
  <div class="chips" style="margin-top:4px">
    <span class="chip on">Más conveniente</span><span class="chip">Menor precio</span>
    <span class="chip">Más cercano</span><span class="chip">Menor flete</span>
  </div>
  <div class="hr" style="margin-top:10px"></div>
</div>
<div class="list">{products_html}</div>
{cart_html}
<div class="navbar"></div>
"""


def product_card(p, offer, stamp=False, loc=None):
    stamp_html = f'<span class="stamp">Añadido</span>' if stamp else ""
    flete = offer["fee"]
    flete_txt = "Flete S/ 3.50" if flete <= 3.50 else f"Flete S/ {flete:.2f}"
    diff = datos.highest_total(p, loc) - offer["total"]
    savings = (f'<div class="savings">Ahorras S/ {diff:.2f} comprando aquí frente a otros distribuidores</div>'
               if diff >= 1.5 else "")
    return f"""
<div class="card">
  <div class="top">
    <div class="plate"><img src="{img_url(p['image'])}">{stamp_html}</div>
    <div style="flex:1;min-width:0;margin-left:14px">
      <div class="overline" style="font-size:10.5px">{html.escape(p['category'].upper())} · {html.escape(p['brand'])}</div>
      <div class="pname">{html.escape(p['name'])}</div>
      <div class="pres">{html.escape(p['presentation'])}</div>
      <div class="sup"><span class="supname">{html.escape(offer['supplier'])}</span>
        <span class="pill-rule">{offer['km']:.1f} km</span></div>
    </div>
  </div>
  <div class="hr2"></div>
  <div class="row" style="gap:10px">
    <span class="price">{money(offer['price'])}</span>
    <span class="pill-sage">{flete_txt}</span>
  </div>
  <div style="font:400 12px Roboto,Arial,sans-serif;color:{c('ink_muted')};margin-top:2px">Total en tu local: {money(offer['total'])}</div>
  <div class="btnrow">
    <div class="btn-out">{icon('ic_swap', 18, c('ink'))} Comparar ({p['n_offers']})</div>
    <div class="btn-fill">{icon('ic_add', 18, c('paper_on_ink'))} Pedir</div>
  </div>
  {savings}
</div>"""


def cart_bar(units, total, flete):
    return f"""
<div class="cartbar">
  <div style="flex:1;min-width:0">
    <div class="t">{units} ítems · {money(total)}</div>
    <div class="s">Incluye {money(flete)} de flete a {html.escape(datos.LOCATIONS[0]['name'])}</div>
  </div>
  <div class="cartbtn">Ver pedido {icon('ic_arrow_forward', 18, c('ink'))}</div>
</div>"""


# ---------------------------------------------------------------- pantallas
def build():
    os.makedirs(OUT_HTML, exist_ok=True)
    loc = datos.LOCATIONS[0]
    sporade = datos.product("p_sporade")
    arroz = datos.product("p_arroz")

    so = datos.best_offer(sporade, loc)
    ao = datos.best_offer(arroz, loc)

    # Pantalla principal con dos hojas y barra de pedido
    cart_units = 3
    cart_sub = so["price"] * 2 + ao["price"]
    cart_flete = datos.cart_fee([(so, 2), (ao, 1)], loc)
    prods = (product_card(dict(sporade, n_offers=len(sporade["offers"])), so, stamp=True, loc=loc)
             + product_card(dict(arroz, n_offers=len(arroz["offers"])), ao, stamp=False, loc=loc))
    main_body = base_screen(cart_bar(cart_units, cart_sub + cart_flete, cart_flete), prods)
    open(os.path.join(OUT_HTML, "01_pantalla_principal.html"), "w", encoding="utf-8").write(page(main_body))

    # Fondo común para hojas: pantalla principal atenuada
    backdrop = main_body

    # Comparador de proveedores (Sporade x2)
    offers = sorted([datos.offer_view(sporade, o, loc, 2) for o in sporade["offers"]], key=lambda x: x["total"])
    best_total_idx = 0
    rows = ""
    for i, o in enumerate(offers):
        badge = ""
        cls = "block"
        if i == 0:
            cls = "block active"
            badge = '<span class="pill-gilt">Mejor total</span>'
        free = o["subtotal"] >= o["free"]
        ship = (f'Envío gratis aplicado · superó S/ {o["free"]:.0f}' if free
                else f'{o["time"]} · Envío gratis desde S/ {o["free"]:.0f} (faltan S/ {o["free"]-o["subtotal"]:.2f})')
        flete_txt = "GRATIS" if free else f"S/ {o['fee']:.2f}"
        flete_col = c('sage') if free else c('ink')
        rows += f"""
<div class="{cls}" style="margin-bottom:10px">
  <div class="row between">
    <div style="min-width:0"><div style="font:600 17px EBG,Georgia,serif;color:{c('ink')}">{html.escape(o['supplier'])}</div>
      <div style="font:400 12px Roboto,Arial,sans-serif;color:{c('ink_muted')};margin-top:2px">{o['km']:.1f} km · {html.escape(o['district'])} · {html.escape(sin_emoji(o['badge']))}</div></div>
    {badge}
  </div>
  <div style="font:400 13px Roboto,Arial,sans-serif;color:{c('ink_soft')};margin-top:6px">{html.escape(o['note'])} · Stock: {o['stock']} unid.</div>
  <div style="font:500 12px Roboto,Arial,sans-serif;color:{c('sage') if free else c('ink_muted')};margin-top:2px">{html.escape(ship)}</div>
  <div class="blockdeep row between" style="margin-top:10px">
    <div><div class="label">Producto</div><div class="money" style="margin-top:2px">{money(o['subtotal'])}</div></div>
    <div style="color:{c('ink_muted')}">+</div>
    <div><div class="label">Flete</div><div class="money" style="margin-top:2px;color:{flete_col}">{flete_txt}</div></div>
    <div style="color:{c('ink_muted')}">=</div>
    <div><div class="label" style="color:{c('gilt_dark')}">Costo final</div><div class="price" style="font-size:19px;margin-top:2px">{money(o['total'])}</div></div>
  </div>
  <div style="height:42px;margin-top:12px;border-radius:10px;background:{c('ink')};color:{c('paper_on_ink')};display:flex;align-items:center;justify-content:center;gap:6px;font:500 14px Roboto,Arial,sans-serif">{icon('ic_add', 18, c('paper_on_ink'))} Añadir al pedido</div>
</div>"""
    compare = f"""
<div class="scrim"></div>
<div class="sheet" style="top:70px">
  <div class="handle"></div>
  <div class="row between" style="margin-top:10px">
    <div class="row" style="gap:12px">
      <div style="width:64px;height:64px;border-radius:4px;overflow:hidden;border:.5px solid {c('rule_strong')};background:{c('paper_deep')}"><img src="{img_url(sporade['image'])}" style="width:100%;height:100%;object-fit:cover;filter:saturate(.84)"></div>
      <div><div class="pname" style="margin:0;font-size:20px">{html.escape(sporade['name'])}</div>
        <div class="pres">{html.escape(sporade['presentation'])} · {html.escape(sporade['brand'])}</div></div>
    </div>
    <div class="iconbtn">{icon('ic_close', 20, c('ink_muted'))}</div>
  </div>
  <div class="row" style="margin-top:12px;background:{c('paper_deep')};border-radius:4px;padding:10px;gap:8px">
    {icon('ic_place', 18, c('gilt'))}<span class="body" style="color:{c('ink')};font-weight:500;font-size:13px">Cotizando fletes a: {html.escape(loc['name'])}</span>
  </div>
  <div class="block active row between" style="margin-top:12px;padding:10px 8px 10px 14px">
    <div><div class="label" style="color:{c('gilt_dark')};font-size:11px">Cantidad a cotizar</div>
      <div class="body" style="font-size:12px;margin-top:2px">Sube la cantidad para alcanzar el envío gratis</div></div>
    <div class="stepper">
      <div class="stepbtn">{icon('ic_remove', 18, c('ink'))}</div>
      <div class="qty">2</div>
      <div class="stepbtn fill">{icon('ic_add', 18, c('paper_on_ink'))}</div>
    </div>
  </div>
  <div class="label" style="margin-top:16px;margin-bottom:8px">Mayoristas ordenados por conveniencia</div>
  {rows}
</div>"""
    open(os.path.join(OUT_HTML, "02_comparador_proveedores.html"), "w", encoding="utf-8").write(
        page(backdrop + compare))

    # Hoja de pedido
    cart_items = [
        (sporade, so, 2),
        (arroz, ao, 1),
    ]
    item_rows = ""
    for prod, off, q in cart_items:
        sub = off["price"] * q
        item_rows += f"""
<div class="block" style="margin-bottom:10px;padding:12px">
  <div class="row" style="gap:12px;align-items:center">
    <div style="width:56px;height:56px;border-radius:4px;overflow:hidden;border:.5px solid {c('rule_strong')};background:{c('paper_deep')};flex:none"><img src="{img_url(prod['image'])}" style="width:100%;height:100%;object-fit:cover;filter:saturate(.84)"></div>
    <div style="flex:1;min-width:0">
      <div style="font:600 16px/1.2 EBG,Georgia,serif;color:{c('ink')}">{html.escape(prod['name'])}</div>
      <div class="body" style="font-size:12px;color:{c('ink_muted')};margin-top:2px">{html.escape(off['supplier'])}</div>
      <div class="body" style="font-size:12px;color:{c('ink_muted')}">{money(off['price'])} c/u</div>
    </div>
    <div style="font:700 18px EBG,Georgia,serif;color:{c('ink')}">{money(sub)}</div>
  </div>
  <div class="row" style="margin-top:8px;gap:8px">
    <div class="stepbtn" style="width:40px;height:40px">{icon('ic_remove', 18, c('ink'))}</div>
    <div class="qty" style="width:44px;font-size:18px">{q}</div>
    <div class="stepbtn" style="width:40px;height:40px">{icon('ic_add', 18, c('ink'))}</div>
    <div style="flex:1"></div>
    <div style="font:500 13px Roboto,Arial,sans-serif;color:{c('stamp')};display:flex;align-items:center;gap:4px">{icon('ic_delete', 18, c('stamp'))} Quitar</div>
  </div>
</div>"""
    flete_all = datos.cart_fee([(so, 2), (ao, 1)], loc)
    sub_all = sum(off["price"] * q for _, off, q in cart_items)
    cart_sheet = f"""
<div class="scrim"></div>
<div class="sheet" style="top:40px">
  <div class="handle"></div>
  <div class="row between" style="margin-top:10px">
    <div><div class="overline">Resumen de abastecimiento</div><div class="display" style="margin-top:2px">Tu pedido</div></div>
    <div class="iconbtn">{icon('ic_close', 20, c('ink_muted'))}</div>
  </div>
  <div class="hr" style="margin-top:12px"></div>
  <div class="row" style="margin-top:12px;gap:8px">{icon('ic_place', 18, c('gilt'))}<span class="body" style="color:{c('ink')};font-weight:500;font-size:13px">Destino: {html.escape(loc['name'])}</span></div>
  <div style="margin-top:12px">{item_rows}</div>
  <div class="block" style="padding:14px;margin-top:4px">
    <div class="row between"><span class="body">Productos</span><span class="money" style="font-size:14px">{money(sub_all)}</span></div>
    <div class="row between" style="margin-top:6px"><span class="body">Fletes estimados</span><span class="money" style="font-size:14px;color:{c('sage')}">{money(flete_all)}</span></div>
    <div class="hr" style="margin:10px 0"></div>
    <div class="row between"><span class="overline" style="color:{c('ink')}">Total general</span><span class="price">{money(sub_all + flete_all)}</span></div>
  </div>
  <div style="height:52px;margin-top:16px;border-radius:12px;background:{c('ink')};color:{c('paper_on_ink')};display:flex;align-items:center;justify-content:center;gap:8px;font:500 15px Roboto,Arial,sans-serif">{icon('ic_send', 18, c('paper_on_ink'))} Enviar pedido por WhatsApp</div>
  <div style="text-align:center;margin-top:6px;font:500 13px Roboto,Arial,sans-serif;color:{c('stamp')};display:flex;justify-content:center;align-items:center;gap:4px">{icon('ic_delete', 18, c('stamp'))} Vaciar pedido</div>
</div>"""
    cart_backdrop = main_body
    open(os.path.join(OUT_HTML, "03_hoja_pedido.html"), "w", encoding="utf-8").write(page(cart_backdrop + cart_sheet))

    # Selector de ubicación
    loc_rows = ""
    for l in datos.LOCATIONS:
        active = l["id"] == loc["id"]
        badge = ('<span class="pill-gilt">Actual</span>' if active
                 else f'<span class="pill-rule" style="font-weight:700;letter-spacing:.14em;text-transform:uppercase;font-size:10px">Elegir</span>')
        loc_rows += f"""
<div class="{'block active' if active else 'block'} row" style="margin-bottom:8px;padding:14px;gap:12px">
  {icon('ic_place', 20, c('gilt'))}
  <div style="flex:1;min-width:0">
    <div style="font:600 14px Roboto,Arial,sans-serif;color:{c('ink')}">{html.escape(l['name'])}</div>
    <div class="body" style="font-size:12px;color:{c('ink_muted')};margin-top:2px">{html.escape(l['ref'])}</div>
  </div>
  {badge}
</div>"""
    loc_sheet = f"""
<div class="scrim"></div>
<div class="sheet" style="top:150px">
  <div class="handle"></div>
  <div class="row between" style="margin-top:10px">
    <div><div class="overline">Chiclayo y Lambayeque</div><div class="display" style="margin-top:2px">¿Dónde está tu bodega?</div></div>
    <div class="iconbtn">{icon('ic_close', 20, c('ink_muted'))}</div>
  </div>
  <div class="body" style="margin-top:8px">Elige tu zona para calcular fletes exactos y encontrar qué distribuidor te conviene según la distancia.</div>
  <div class="hr" style="margin:14px 0"></div>
  {loc_rows}
</div>"""
    open(os.path.join(OUT_HTML, "04_selector_ubicacion.html"), "w", encoding="utf-8").write(page(backdrop + loc_sheet))

    print("HTML generado en", OUT_HTML)


if __name__ == "__main__":
    build()
