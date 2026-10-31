#!/usr/bin/env python3
"""Carga una presentación por la API pública, sin saltarse las reglas del negocio.

Solicita las contraseñas, o usa AGROMARKET_DEMO_BUYER_PASSWORD y
AGROMARKET_DEMO_PROVIDER_PASSWORD para ejecución automatizada.
AGROMARKET_API_URL permite seleccionar otro entorno. Se puede repetir sin duplicar
publicaciones, reservas, mensajes ni reseñas. Nunca elimina datos existentes.
"""
import json
import os
from datetime import date, timedelta
from getpass import getpass
from urllib.error import HTTPError
from urllib.request import Request, urlopen

BASE = os.environ.get('AGROMARKET_API_URL', 'https://agromarket-api-wkwselgnga-uc.a.run.app').rstrip('/')


def api(path, account=None, method='GET', body=None):
    headers = {'Content-Type': 'application/json'}
    if account:
        headers['Authorization'] = 'Bearer ' + account['accessToken']
    request = Request(BASE + path, headers=headers, method=method,
                      data=None if body is None else json.dumps(body).encode())
    try:
        with urlopen(request, timeout=60) as response:
            raw = response.read()
            return json.loads(raw) if raw else None
    except HTTPError as error:
        detail = error.read().decode()
        raise RuntimeError(f'{method} {path}: {error.code} {detail}') from None


def login(kind):
    email = ('comprador' if kind == 'BUYER' else 'proveedor') + '.pruebas@agromarket.test'
    password = os.environ.get('AGROMARKET_DEMO_' + kind + '_PASSWORD') or getpass('Contraseña de ' + email + ': ')
    return api('/api/auth/login', method='POST', body={'email': email, 'password': password})


def review(path, account, body):
    try:
        api(path, account, 'POST', body)
    except RuntimeError as error:
        if 'ya tiene una reseña' not in str(error):
            raise


def main():
    buyer, provider = login('BUYER'), login('PROVIDER')
    categories = api('/api/categories', buyer)
    catalog = {p['code']: (c['id'], p['id']) for c in categories for p in c['products']}
    units = {u['code']: u['id'] for u in api('/api/unit-types', buyer)}
    offers = [
        ('CORN', 'Maíz amarillo seco de Zacapa', 185, 'QUINTAL', 'Zacapa'),
        ('BEANS', 'Frijol negro seleccionado de Chiquimula', 480, 'QUINTAL', 'Chiquimula'),
        ('COFFEE', 'Café pergamino de Huehuetenango', 1450, 'QUINTAL', 'Huehuetenango'),
        ('VEGETABLES', 'Tomate fresco para restaurantes', 7.5, 'LIBRA', 'Chimaltenango'),
        ('PET_PLASTIC', 'PET transparente clasificado y limpio', 3.25, 'KILOGRAMO', 'Guatemala'),
        ('CARDBOARD', 'Cartón corrugado compactado', 1.8, 'KILOGRAMO', 'Escuintla'),
        ('ALUMINUM', 'Aluminio separado para reciclaje', 9.5, 'KILOGRAMO', 'Guatemala'),
        ('GLASS', 'Vidrio transparente clasificado', 650, 'TONELADA', 'Sacatepéquez'),
        ('OTHER_AGRICULTURAL', 'Aguacate Hass de cosecha local', 12, 'LIBRA', 'Sololá'),
        ('CORN', 'Maíz blanco para tortillerías', 195, 'QUINTAL', 'Jutiapa'),
        ('BEANS', 'Frijol rojo de cosecha reciente', 525, 'QUINTAL', 'Jalapa'),
        ('COFFEE', 'Café de altura de Antigua', 1680, 'QUINTAL', 'Sacatepéquez'),
        ('VEGETABLES', 'Zanahoria lavada por mayor', 4.25, 'LIBRA', 'Chimaltenango'),
        ('PET_PLASTIC', 'PET de color listo para acopio', 2.5, 'KILOGRAMO', 'Quetzaltenango'),
        ('CARDBOARD', 'Cartón seco para recolección semanal', 1.6, 'KILOGRAMO', 'Guatemala'),
        ('ALUMINUM', 'Latas de aluminio compactadas', 10.25, 'KILOGRAMO', 'Escuintla'),
        ('GLASS', 'Vidrio ámbar separado por color', 580, 'TONELADA', 'Guatemala'),
        ('VEGETABLES', 'Cebolla blanca seleccionada', 5.75, 'LIBRA', 'Sololá'),
        ('CORN', 'Maíz amarillo para distribuidores', 180, 'QUINTAL', 'Petén'),
        ('BEANS', 'Frijol negro para abarroterías', 465, 'QUINTAL', 'Alta Verapaz'),
        ('COFFEE', 'Café pergamino de Cobán', 1390, 'QUINTAL', 'Alta Verapaz'),
        ('VEGETABLES', 'Papa fresca de San Marcos', 4.5, 'LIBRA', 'San Marcos'),
        ('PET_PLASTIC', 'Botellas PET para transformación', 3, 'KILOGRAMO', 'Zacapa'),
        ('OTHER_AGRICULTURAL', 'Limón persa para comercios', 2.25, 'UNIDAD', 'Escuintla'),
    ]
    accounts = [buyer, provider]
    existing = []
    for account in accounts:
        page = api('/api/publications/mine?size=100', account)
        existing.append(page['content'] if isinstance(page, dict) else page)
    publications = []
    for i, (code, title, price, unit, location) in enumerate(offers):
        owner = 1 if i < 18 else 0
        publication = next((p for p in existing[owner] if p['title'] == title), None)
        if not publication:
            category, product = catalog[code]
            publication = api('/api/publications', accounts[owner], 'POST', {
                'categoryId': category, 'productId': product, 'title': title,
                'description': 'DATOS FICTICIOS DE DEMOSTRACIÓN · Seed presentación v1. '
                    'Lote seleccionado para venta mayorista. Entrega coordinada por mensajería; '
                    'consulta disponibilidad, empaque y punto de retiro antes de reservar.',
                'price': price, 'units': 120 + i * 15, 'unitTypeId': units[unit],
                'location': location,
                'availableDate': (date.today() + timedelta(days=7 + i)).isoformat() if i % 4 == 3 else None,
            })
        publications.append((publication, owner))
    print('Publicaciones de presentación: 24', flush=True)

    # Deja acciones disponibles y ejemplos de cada fase comercial en ambas cuentas.
    targets = ['COMPLETED', 'COMPLETED', 'COMPLETED', 'DELIVERED', 'READY_FOR_DELIVERY',
               'CONFIRMED', 'ACCEPTED_PENDING_PAYMENT', 'PENDING_PROVIDER',
               'PENDING_PROVIDER', 'REJECTED', 'CANCELLED', 'CONFIRMED']
    path = ['PENDING_PROVIDER', 'ACCEPTED_PENDING_PAYMENT', 'CONFIRMED',
            'READY_FOR_DELIVERY', 'DELIVERED', 'COMPLETED']
    actions = [('accept', 1), ('confirm', 0), ('ready', 1), ('delivered', 1), ('complete', 0)]
    reservations = []
    for index, (publication, owner) in enumerate(publications[:12] + publications[18:21]):
        seller, customer = accounts[owner], accounts[1-owner]
        target = targets[index] if index < 12 else ['COMPLETED', 'ACCEPTED_PENDING_PAYMENT', 'PENDING_PROVIDER'][index-12]
        existing_reservations = api('/api/reservations/mine', customer)
        reservation = next((r for r in existing_reservations if r['publicationId'] == publication['id']), None)
        if not reservation:
            reservation = api('/api/reservations', customer, 'POST', {'publicationId': publication['id'], 'quantity': 3 + index % 8})
        if reservation['status'] != target:
            if target in ['REJECTED', 'CANCELLED'] and reservation['status'] == 'PENDING_PROVIDER':
                reservation = api('/api/reservations/'+reservation['id']+('/reject' if target=='REJECTED' else '/cancel'), seller if target=='REJECTED' else customer, 'PATCH')
            elif target in path and reservation['status'] in path:
                while path.index(reservation['status']) < path.index(target):
                    action, actor = actions[path.index(reservation['status'])]
                    reservation = api('/api/reservations/'+reservation['id']+'/'+action, seller if actor else customer, 'PATCH')
        reservations.append(reservation)
        if reservation['status'] == 'COMPLETED':
            review('/api/reviews/products', customer, {'reservationId': reservation['id'], 'rating': 5 if index%2==0 else 4,
                'comment': 'Reseña de demostración: lote recibido según lo acordado, buena calidad y entrega puntual.'})

    for index, (publication, owner) in enumerate(publications[:8] + publications[18:21]):
        seller, customer = accounts[owner], accounts[1-owner]
        conversation = api('/api/conversations', customer, 'POST', {'publicationId': publication['id']})
        old = api('/api/conversations/'+conversation['id']+'/messages', customer)
        messages = [
            (customer, 'Hola, me interesa '+publication['title']+'. ¿Podemos coordinar la entrega?'),
            (seller, '¡Hola! Sí, tenemos disponibilidad. Podemos coordinar el retiro en '+publication['location']+'.'),
            (customer, 'Perfecto. Revisaré la cantidad y enviaré la reserva desde la aplicación.'),
            (seller, 'Con gusto. Quedo pendiente de tu reserva. Conversación de demostración.'),
        ]
        for actor, content in messages:
            if not any(m['content']==content and m['senderId']==actor['user']['id'] for m in old):
                api('/api/conversations/'+conversation['id']+'/messages', actor, 'POST', {'content': content})
        if index < 6 or index >= 8:
            review('/api/reviews/providers', customer, {'conversationId': conversation['id'], 'purchased': index < 3 or index==8,
                'rating': 5 if index%3 else 4, 'comment': 'Reseña de demostración: atención clara, respuesta rápida y buena coordinación.'})

    for publication, owner in publications[:6] + publications[18:24]:
        api('/api/favorites/'+publication['id'], accounts[1-owner], 'POST')

    print(json.dumps({'publications': len(publications), 'reservations': len(reservations),
                      'conversations': 11, 'messages': 44, 'providerReviews': 9, 'productReviews': 4,
                      'favorites': 12}, ensure_ascii=False), flush=True)
    for account in accounts:
        print(account['user']['email'], json.dumps(api('/api/analytics/me', account), ensure_ascii=False), flush=True)


if __name__ == '__main__':
    main()
