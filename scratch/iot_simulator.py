import requests
import time
import random
import json

# Configuración del Simulador
BASE_URL = "http://localhost:8080/api/mecatronic"
API_KEY = "MOCK-API-KEY-123" # Cambiar por una real tras vinculación
INTERVAL = 5 # Segundos entre telemetría

def send_telemetry():
    # Simulamos sensores de un sistema de riego
    data = {
        "TEMP": f"{random.uniform(20.0, 30.0):.2f}",
        "HUMIDITY": f"{random.uniform(40.0, 60.0):.2f}",
        "SOIL_MOISTURE": f"{random.randint(10, 90)}",
        "WATER_FLOW": f"{random.uniform(0.0, 2.5):.2f}",
        "PUMP_STATUS": "OFF"
    }
    
    print(f"[*] Enviando telemetría: {data}")
    try:
        response = requests.post(
            f"{BASE_URL}/telemetry",
            params={"apiKey": API_KEY},
            json=data
        )
        if response.status_code == 200:
            print("[+] Telemetría enviada con éxito.")
        else:
            print(f"[-] Error al enviar telemetría: {response.status_code}")
    except Exception as e:
        print(f"[!] Error de conexión: {e}")

def check_commands():
    print("[*] Consultando comandos pendientes...")
    try:
        response = requests.get(
            f"{BASE_URL}/commands",
            params={"apiKey": API_KEY}
        )
        if response.status_code == 200:
            commands = response.json()
            if commands:
                print(f"[!] COMANDO RECIBIDO: {commands}")
                # Aquí el ESP32 activaría el Relé
            else:
                print("[.] No hay comandos pendientes.")
        else:
            print(f"[-] Error al consultar comandos: {response.status_code}")
    except Exception as e:
        print(f"[!] Error de conexión: {e}")

if __name__ == "__main__":
    print("=== BelMarket IoT Simulator (ESP32) ===")
    print(f"Conectando a: {BASE_URL}")
    print(f"Usando API Key: {API_KEY}")
    print("-" * 40)
    
    try:
        while True:
            send_telemetry()
            time.sleep(1) # Pequeña espera entre polling
            check_commands()
            print(f"Esperando {INTERVAL} segundos...")
            time.sleep(INTERVAL)
    except KeyboardInterrupt:
        print("\n[!] Simulador detenido por el usuario.")
