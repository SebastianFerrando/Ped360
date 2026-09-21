import { Injectable, computed, signal } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo } from '@azure/msal-browser';
import { environment } from '../../environments/environment';

export type RolApp = 'Admin' | 'Operador' | 'Cliente';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly _cuenta = signal<AccountInfo | null>(null);

  readonly cuenta = this._cuenta.asReadonly();
  readonly nombreUsuario = computed(() => this._cuenta()?.username ?? '');
  readonly estaAutenticado = computed(() => this._cuenta() !== null);

  /**
   * Entra ID incluye los App Roles asignados dentro del claim "roles" del
   * ID Token (y del Access Token), tal como se configuró en el paso de
   * "App roles" de la app registration del backend.
   */
  readonly roles = computed<RolApp[]>(() => {
    const claims = this._cuenta()?.idTokenClaims as { roles?: RolApp[] } | undefined;
    return claims?.roles ?? [];
  });

  readonly esAdmin = computed(() => this.roles().includes('Admin'));
  readonly esOperador = computed(() => this.roles().includes('Operador'));
  readonly esCliente = computed(() => this.roles().includes('Cliente'));
  /** Admin/Operador comparten las pantallas de gestión operativa de pedidos. */
  readonly puedeGestionarPedidos = computed(() => this.esAdmin() || this.esOperador());

  constructor(private msalService: MsalService) {}

  inicializarCuentaActiva(): void {
    let cuenta = this.msalService.instance.getActiveAccount();
    if (!cuenta && this.msalService.instance.getAllAccounts().length > 0) {
      cuenta = this.msalService.instance.getAllAccounts()[0];
      this.msalService.instance.setActiveAccount(cuenta);
    }
    this._cuenta.set(cuenta);
  }

  login(): void {
    this.msalService.loginRedirect({ scopes: [environment.azure.scope] });
  }

  logout(): void {
    this.msalService.logoutRedirect();
  }

  tieneRol(rol: RolApp): boolean {
    return this.roles().includes(rol);
  }
}
