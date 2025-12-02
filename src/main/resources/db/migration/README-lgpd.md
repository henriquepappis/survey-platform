## LGPD / Privacidade

- Este backend armazena IP, user-agent e dados de localização enviados pelo frontend em `ResponseSession`.
- Em produção, recomendamos anonimizar/truncar IP (ex.: remover último octeto ou aplicar hash irreversível com salt) e definir política de retenção (ex.: purge após 90 dias).
- Ajuste o frontend para exibir consentimento e só enviar campos opcionais de audiência quando autorizado.
- Se desejar, implemente um `ResponseSessionPrivacyService` para mascarar IP antes de persistir e configurar um job de limpeza periódica.
