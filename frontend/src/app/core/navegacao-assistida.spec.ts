import { registrarNavegacao } from './navegacao-assistida';
import { vi } from 'vitest';

describe('Navegação assistida opcional', () => {
  it('não exige suporte do navegador', () => { expect(() => registrarNavegacao({}, () => false, async () => true)()).not.toThrow(); });
  it('valida entrada, exige sessão, navega e remove registro ao encerrar', async () => {
    let tool!: Parameters<NonNullable<Parameters<typeof registrarNavegacao>[0]['modelContext']>['registerTool']>[0];
    let signal!: AbortSignal; let signedIn = false;
    const navegar = vi.fn(async () => true);
    const cleanup = registrarNavegacao({ modelContext: { registerTool(t, options) { tool = t; signal = options.signal; } } }, () => signedIn, navegar);
    expect(tool.name).toBe('abrir_carteira'); expect(tool.annotations.readOnlyHint).toBe(false);
    await expect(tool.execute({ usuarioId: 1 })).rejects.toThrow('objeto vazio');
    await expect(tool.execute({})).rejects.toThrow('Entre na sua conta'); expect(navegar).not.toHaveBeenCalled();
    signedIn = true; await expect(tool.execute({})).resolves.toEqual({ pagina: 'carteira' });
    expect(navegar).toHaveBeenCalledOnce(); cleanup(); expect(signal.aborted).toBe(true);
  });
});
