# REFRICAR — Gestão de Orçamentos

Sistema de criação e gerenciamento de orçamentos da **REFRICAR** (instalação e manutenção de ar-condicionado).

- Cadastro de clientes, serviços (mão de obra) e materiais com preços
- Tabela de preços com edição rápida e reajuste em %
- Modelos de orçamentos prontos (criar, editar, duplicar, excluir)
- Novo orçamento a partir de modelo, com cálculo automático (materiais, mão de obra, serviços adicionais, taxas, descontos e total)
- Cada orçamento guarda os preços do dia em que foi feito — mudar a tabela não altera orçamentos antigos
- PDF profissional (azul-marinho/azul/branco), envio pelo WhatsApp
- Histórico com status (em elaboração, enviado, aprovado, recusado, concluído), pesquisa por cliente, número e período
- Banco de dados no **Supabase** com login; backup/importação em JSON e exportação em planilha (CSV)
- Funciona no computador, tablet e celular (pode ser instalado na tela inicial do celular)

## Arquivos

| Arquivo | Para que serve |
|---|---|
| `index.html` | O aplicativo inteiro |
| `config.js` | Endereço e chave pública do Supabase |
| `supabase/schema.sql` | Cria a tabela e as regras de segurança no Supabase |
| `manifest.webmanifest`, `icon-*.png` | Ícone e instalação no celular |

## Configuração do Supabase (uma vez só)

1. Em [supabase.com](https://supabase.com), crie um projeto (o plano gratuito serve).
2. **SQL Editor → New query**: cole todo o conteúdo de `supabase/schema.sql` e clique em **Run**.
3. **Authentication → Users → Add user → Create new user**: informe seu e-mail e uma senha e marque **Auto Confirm User**.
4. **Authentication → Sign In / Providers**: desligue **Allow new users to sign up**, para ninguém mais criar conta.
5. **Project Settings → API**: copie a **Project URL** e a chave **anon / publishable** e coloque em `config.js`.
   *Nunca* use a chave `service_role` / `secret` no site.

Se `config.js` estiver vazio, o próprio site pede a URL e a chave na primeira vez que for aberto.

## Publicação (GitHub Pages)

**Settings → Pages → Build and deployment → Source: Deploy from a branch → Branch: `main` / `(root)` → Save.**
O site fica em `https://<seu-usuario>.github.io/REFRICAR/`.

## Aplicativo Android (APK)

**Baixar:** https://github.com/scoffyt/REFRICAR/releases/latest/download/REFRICAR.apk

- O app abre o site publicado no GitHub Pages, então **tudo que for enviado ao GitHub aparece no app sozinho**, sem reinstalar.
- No app, o PDF é salvo em `Downloads/REFRICAR` e o botão do WhatsApp já envia o PDF anexado na conversa do cliente.
- O APK é gerado pelo GitHub Actions (`.github/workflows/android.yml`) sempre que algo muda na pasta `android/`,
  ou manualmente em **Actions → Gerar APK Android → Run workflow**.
- Assinatura: cadastre em **Settings → Secrets and variables → Actions** os segredos `REFRICAR_KEYSTORE_BASE64`
  e `REFRICAR_KEYSTORE_PASSWORD`. Sem eles o APK sai com assinatura provisória (para instalar uma versão nova
  do APK será preciso desinstalar a anterior). A chave nunca deve ser enviada ao repositório.

## Segurança

A chave `anon` é pública por natureza. Os dados ficam protegidos porque:
- só quem faz login enxerga ou altera alguma coisa;
- as regras RLS do `schema.sql` deixam cada usuário acessar apenas os próprios registros.

## Backup

Em **Configurações da empresa → Banco de dados e backup** você exporta um arquivo `.json` com todos os dados
e pode importá-lo de volta (inclusive para passar dados de outra versão do aplicativo).
