# Bedrock Runbook Diagram Generator

This Scala application reads a runbook from Google Docs and uses AWS Bedrock to generate a Mermaid diagram showing the decision-making process for support engineers.

## What It Does

1. Reads your runbook from Google Docs
2. Loads the prompt template from `prompt.txt`
3. Sends the content to AWS Bedrock
4. Generates a Mermaid diagram showing:
   - What symptoms to observe
   - Decision points for support engineers
   - Remediation actions to take
   - Explanatory notes attached to nodes
   - Clickable links to dashboards and documentation
   - **Sub-diagrams for complex actions** - detailed how-to steps with links
5. Outputs valid Mermaid syntax
6. **Saves the diagram to a timestamped markdown file** with a link to the source Google Doc

## Environment Variables

Create a `.env` file in the project root with the following variables:

```bash
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-YourClientSecret
GOOGLE_REFRESH_TOKEN=1//0YourRefreshToken...
```

The Google Doc URL is passed as a command-line argument:
```bash
sbt "run https://docs.google.com/document/d/YOUR_DOC_ID/edit"
```

## Customising the Prompt

The prompt sent to Bedrock is stored in **`prompt.txt`**. You can edit this file to:
- Change diagram requirements
- Add or remove syntax constraints  
- Modify how notes and links are added
- Adjust the output format
- Change the diagram type (flowchart, state diagram, etc.)

The file uses `{RUNBOOK_CONTENT}` as a placeholder that gets replaced with your Google Doc content.

## Output

The program generates a timestamped markdown file (e.g., `runbook-diagram-2025-11-04_14-30-45.md`) containing:
- Generation timestamp
- Link to the original Google Doc
- The Mermaid diagram code
