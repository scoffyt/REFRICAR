-- REFRICAR — Gestão de Orçamentos
-- Cole este arquivo inteiro no Supabase → SQL Editor → New query → Run.
-- Cria a tabela única onde o aplicativo guarda clientes, serviços, materiais,
-- modelos, orçamentos e configurações da empresa. Cada usuário só enxerga os próprios dados.

create table if not exists public.registros (
  owner         uuid        not null default auth.uid() references auth.users(id) on delete cascade,
  colecao       text        not null,   -- clientes | servicos | materiais | modelos | orcamentos | config
  id            text        not null,
  dados         jsonb       not null default '{}'::jsonb,
  atualizado_em timestamptz not null default now(),
  primary key (owner, colecao, id)
);

create index if not exists registros_owner_colecao_idx on public.registros (owner, colecao);

alter table public.registros enable row level security;

drop policy if exists "registros: ler os proprios"    on public.registros;
drop policy if exists "registros: inserir os proprios" on public.registros;
drop policy if exists "registros: alterar os proprios" on public.registros;
drop policy if exists "registros: excluir os proprios" on public.registros;

create policy "registros: ler os proprios"    on public.registros for select to authenticated using (auth.uid() = owner);
create policy "registros: inserir os proprios" on public.registros for insert to authenticated with check (auth.uid() = owner);
create policy "registros: alterar os proprios" on public.registros for update to authenticated using (auth.uid() = owner) with check (auth.uid() = owner);
create policy "registros: excluir os proprios" on public.registros for delete to authenticated using (auth.uid() = owner);

revoke all on public.registros from anon;
grant select, insert, update, delete on public.registros to authenticated;
