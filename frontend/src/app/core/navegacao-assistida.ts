interface Ferramenta {
  name: string; title: string; description: string;
  inputSchema: object;
  annotations: { readOnlyHint: boolean; untrustedContentHint: boolean };
  execute(input: unknown): Promise<{ pagina: string }>;
}
interface DocumentoComFerramentas {
  modelContext?: { registerTool(tool: Ferramenta, options: { signal: AbortSignal }): void | Promise<void> };
}

// Melhoria opcional: navegadores sem WebMCP mantêm exatamente a mesma interface.
export function registrarNavegacao(documento: DocumentoComFerramentas, autenticado: () => boolean, abrir: () => Promise<boolean>): () => void {
  const controller = new AbortController();
  const context = documento.modelContext;
  if (context?.registerTool) {
    try {
      void Promise.resolve(context.registerTool({
        name: 'abrir_carteira', title: 'Abrir minha carteira',
        description: 'Navega para a carteira da sessão já autenticada. Não realiza login nem registra operações.',
        inputSchema: { type: 'object', properties: {}, additionalProperties: false },
        annotations: { readOnlyHint: false, untrustedContentHint: false },
        async execute(input) {
          if (!input || typeof input !== 'object' || Array.isArray(input) || Object.keys(input).length > 0) throw new Error('Informe um objeto vazio.');
          if (!autenticado()) throw new Error('Entre na sua conta pela interface antes de abrir a carteira.');
          if (!await abrir()) throw new Error('Não foi possível abrir a carteira.');
          return { pagina: 'carteira' };
        },
      }, { signal: controller.signal })).catch(() => undefined);
    } catch { /* Suporte opcional não deve interromper o aplicativo. */ }
  }
  return () => controller.abort();
}
