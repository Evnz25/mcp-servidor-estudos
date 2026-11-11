CREATE TABLE IF NOT EXISTS questao (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    enunciado TEXT NOT NULL CHECK (enunciado ~ '[^[:space:]]'),
    alternativas TEXT[] NOT NULL,
    gabarito INTEGER NOT NULL,

    -- O gabarito usa a posição da alternativa, começando em 1.
    CONSTRAINT questao_formato_das_alternativas CHECK (
        cardinality(alternativas) >= 2
        AND array_ndims(alternativas) = 1
        AND array_lower(alternativas, 1) = 1
    ),
    CONSTRAINT questao_alternativas_preenchidas CHECK (
        array_position(alternativas, NULL) IS NULL
        AND array_position(alternativas, '') IS NULL
    ),
    CONSTRAINT questao_gabarito_valido CHECK (
        gabarito BETWEEN 1 AND cardinality(alternativas)
    )
);
