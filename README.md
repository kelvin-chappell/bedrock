# Bedrock Runbook Diagram Generator

This Scala application reads a runbook from Google Docs and uses AWS Bedrock to generate a Mermaid diagram showing the decision-making process for support engineers.

## What It Does

1. Reads your runbook from Google Docs
2. Sends the content to AWS Bedrock
3. Generates a Mermaid diagram showing:
   - What symptoms to observe
   - Decision points for support engineers
   - Remediation actions to take

## Environment Variables

```bash
export GOOGLE_DOC_URL="https://docs.google.com/document/d/YOUR_DOC_ID/edit"
export GOOGLE_CLIENT_ID="your-client-id.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="GOCSPX-YourClientSecret"
export GOOGLE_REFRESH_TOKEN="1//0YourRefreshToken..."
```
