"""
Lee DataRepository.java (proveedores, productos y ofertas) y replica el cálculo
de flete de Supplier.calculateDeliveryFee y DistrictLocation.distanceTo.
"""
import math
import os
import re

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(ROOT, "app", "src", "main", "java", "com", "example",
                   "provedoreschiclayo", "data", "DataRepository.java")
_txt = open(SRC, encoding="utf-8").read()


def _strs(block):
    return re.findall(r'"((?:[^"\\]|\\.)*)"', block)


LOCATIONS = []
for m in re.finditer(r'new DistrictLocation\("([^"]+)", "([^"]+)", "([^"]+)", (-?[\d.]+), (-?[\d.]+)\)', _txt):
    LOCATIONS.append(dict(id=m.group(1), name=m.group(2), ref=m.group(3),
                          lat=float(m.group(4)), lng=float(m.group(5))))

SUPPLIERS = []
for m in re.finditer(r'sups\.add\(new Supplier\((.*?)\)\);', _txt, re.S):
    body = m.group(1)
    s = _strs(body)
    nums = re.findall(r'(?<![\w"])(-?\d+\.\d+|-?\d+)(?![\w"])', re.sub(r'"[^"]*"', '', body))
    SUPPLIERS.append(dict(id=s[0], name=s[1], district=s[2], address=s[3], phone=s[4],
                          lat=float(nums[0]), lng=float(nums[1]), base=float(nums[2]),
                          perKm=float(nums[3]), free=float(nums[4]), time=s[5], badge=s[6]))

_VAR = {"sMosho": 0, "sBalta": 1, "sLamba": 2, "sVictoria": 3, "sBebidas": 4}


def distance(a_lat, a_lng, b_lat, b_lng):
    R = 6371
    dl = math.radians(b_lat - a_lat)
    dn = math.radians(b_lng - a_lng)
    a = math.sin(dl / 2) ** 2 + math.cos(math.radians(a_lat)) * math.cos(math.radians(b_lat)) * math.sin(dn / 2) ** 2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return math.floor(R * c * 10 + 0.5) / 10.0


def fee(sup, loc, subtotal):
    if subtotal >= sup["free"]:
        return 0.0
    d = distance(loc["lat"], loc["lng"], sup["lat"], sup["lng"])
    f = sup["base"] + d * sup["perKm"]
    return math.floor(max(3.0, min(f, 18.0)) * 10.0 + 0.5) / 10.0


PRODUCTS = []
_pmap = {}
for m in re.finditer(r'Product (p\w+) = new Product\((.*?)\);', _txt, re.S):
    s = _strs(m.group(2))
    img = re.search(r'R\.drawable\.(\w+)', m.group(2)).group(1) + ".jpg"
    p = dict(var=m.group(1), id=s[0], name=s[1], category=s[2], brand=s[3],
             presentation=s[4], image=img, offers=[])
    PRODUCTS.append(p)
    _pmap[m.group(1)] = p
for m in re.finditer(r'(p\w+)\.addOffer\(new SupplierOffer\((\w+), ([\d.]+), (\d+), "((?:[^"\\]|\\.)*)"\)\);', _txt):
    _pmap[m.group(1)]["offers"].append(dict(sup=_VAR[m.group(2)], price=float(m.group(3)),
                                            stock=int(m.group(4)), note=m.group(5)))


def product(pid):
    return next(p for p in PRODUCTS if p["id"] == pid)


def offer_view(p, o, loc, qty=1):
    s = SUPPLIERS[o["sup"]]
    sub = o["price"] * qty
    f = fee(s, loc, sub)
    return dict(supplier=s["name"], district=s["district"], badge=s["badge"], time=s["time"],
                note=o["note"], stock=o["stock"], price=o["price"], subtotal=round(sub, 2),
                fee=f, total=round(sub + f, 2), km=distance(loc["lat"], loc["lng"], s["lat"], s["lng"]),
                free=s["free"], sup_index=o["sup"])


def best_offer(p, loc):
    """Oferta más conveniente (producto + flete) para 1 unidad, igual que Product.getMostConvenientOffer."""
    return min((offer_view(p, o, loc) for o in p["offers"]), key=lambda x: x["total"])


def highest_total(p, loc):
    return max(offer_view(p, o, loc)["total"] for o in p["offers"])


def cart_fee(items, loc):
    """items: [(offer_view, qty)]. Agrupa por proveedor como MainActivity.updateCartBar."""
    by_sup = {}
    for off, q in items:
        by_sup.setdefault(off["sup_index"], 0.0)
        by_sup[off["sup_index"]] += off["price"] * q
    return sum(fee(SUPPLIERS[i], loc, sub) for i, sub in by_sup.items())
