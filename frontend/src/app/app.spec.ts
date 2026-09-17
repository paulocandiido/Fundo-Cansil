import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { provideRouter } from '@angular/router';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    })
      .compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('exibe a marca e o atalho de acessibilidade', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.brand')?.textContent).toContain('Fundo Cansil');
    expect(compiled.querySelector('.brand')?.getAttribute('aria-label')).toBe('Fundo Cansil, início');
    expect(compiled.querySelector('.brand-mark')?.textContent).toBe('FC');
    expect(compiled.querySelector('.footer')?.textContent).toContain('Fundo Cansil');
    expect(compiled.querySelector('.skip-link')?.getAttribute('href')).toBe('#conteudo');
  });
});
