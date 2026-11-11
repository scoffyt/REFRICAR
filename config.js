// Conexão com o Supabase (Project Settings → API).
// A chave "anon"/"publishable" é pública por natureza: os dados ficam protegidos
// pelo login e pelas regras de segurança (RLS) do arquivo supabase/schema.sql.
// NUNCA coloque aqui a chave "service_role"/"secret".
window.REFRICAR_CONFIG = {
  supabaseUrl: '',
  supabaseAnonKey: ''
};
