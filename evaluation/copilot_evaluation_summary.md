# Copilot Evaluation

Date: 2026-06-09
Endpoint: `POST /api/copilot/chat`
Role: `POLICE`
Results: `evaluation\copilot_evaluation_results.csv`

## Result

- Total prompts: 12
- Correct intent classifications: 11/12 (91.7%)
- Correct deep links: 11/12 (91.7%)
- Overall correct responses: 11/12 (91.7%)
- Average response time: 2129 ms/prompt

## Notes

The test used controlled operational prompts for vehicle lookup, parking lookup, insurance lookup, review queue, dashboard, audit and navigation. The only failed case was an underspecified navigation prompt, `vreau vehicule`, which returned a help response instead of direct navigation.
