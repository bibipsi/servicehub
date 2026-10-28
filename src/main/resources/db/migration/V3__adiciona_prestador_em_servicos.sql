-- Relaciona cada serviço ao prestador (usuário) responsável por ele.
-- A coluna é opcional para preservar os serviços cadastrados antes da autenticação;
-- esses registros ficam sem prestador e só podem ser alterados por um ADMIN.
ALTER TABLE servicos ADD COLUMN prestador_id BIGINT;

ALTER TABLE servicos
    ADD CONSTRAINT fk_servicos_prestador
        FOREIGN KEY (prestador_id) REFERENCES usuarios (id) ON DELETE RESTRICT;

CREATE INDEX idx_servicos_prestador ON servicos (prestador_id);
