SELECT SUM((length(integra) - length(replace(integra, 'href', ''))) / 4) AS total_hrefs FROM normas;

SELECT SUM((length(integra) - length(replace(integra, 'href="https://leis.org/municipais/sp/sao-paulo/lei/decreto', ''))) / length('href="https://leis.org/municipais/sp/sao-paulo/lei/decreto')) AS total_hrefs FROM normas;

SELECT SUM((length(integra) - length(replace(integra, 'href="https://leis.org/municipais/sp/sao-paulo/lei/lei-ordinaria', ''))) / length('href="https://leis.org/municipais/sp/sao-paulo/lei/lei-ordinaria')) AS total_hrefs FROM normas;

SELECT SUM((length(integra) - length(replace(integra, 'href=https://leis.org/municipais/sp/sao-paulo/lei-organica', ''))) / length('href=https://leis.org/municipais/sp/sao-paulo/lei-organica')) AS total_hrefs FROM normas;


SELECT integra
FROM normas
WHERE integra LIKE '%https://leis.org/municipais/sp/sao-paulo/lei-organica%'
LIMIT 5;

SELECT
    total_hrefs,
    total_decretos,
    total_leis_ordinarias,
	total_leis_organicas,
    total_hrefs - (total_decretos + total_leis_ordinarias + total_leis_organicas) AS diferenca
FROM (
    SELECT
        SUM(
            (length(integra) - length(replace(integra, 'href', ''))) / 4
        ) AS total_hrefs,

        SUM(
            (length(integra) -
             length(replace(
                 integra,
                 'href="https://leis.org/municipais/sp/sao-paulo/lei/decreto',
                 ''
             )))
            / length('href="https://leis.org/municipais/sp/sao-paulo/lei/decreto')
        ) AS total_decretos,

        SUM(
            (length(integra) -
             length(replace(
                 integra,
                 'href="https://leis.org/municipais/sp/sao-paulo/lei/lei-ordinaria',
                 ''
             )))
            / length('href="https://leis.org/municipais/sp/sao-paulo/lei/lei-ordinaria')
        ) AS total_leis_ordinarias,
		
		SUM(
            (length(integra) -
             length(replace(
                 integra,
                 'href=https://leis.org/municipais/sp/sao-paulo/lei-organica',
                 ''
             )))
            / length('href=https://leis.org/municipais/sp/sao-paulo/lei-organica')
        ) AS total_leis_organicas

    FROM normas
);

SELECT
    *
FROM normas
WHERE integra LIKE '%href=%'
  AND integra NOT LIKE '%href="https://leis.org/municipais/sp/sao-paulo/lei/decreto%'
  AND integra NOT LIKE '%href="https://leis.org/municipais/sp/sao-paulo/lei/lei-ordinaria%';
  
  
 select DISTINCT tipoSlug from normas;