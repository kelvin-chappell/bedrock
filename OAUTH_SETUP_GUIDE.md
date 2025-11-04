# How to Get Google OAuth Credentials

This guide explains how to get the OAuth 2.0 credentials needed to run the program.

## Step 1: Create OAuth 2.0 Client Credentials

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select or create a project
3. Enable the Google Docs API:
   - Navigate to **APIs & Services** → **Library**
   - Search for "Google Docs API"
   - Click **Enable**

4. Create OAuth 2.0 credentials:
   - Go to **APIs & Services** → **Credentials**
   - Click **+ CREATE CREDENTIALS** → **OAuth client ID**
   - If prompted, configure the OAuth consent screen:
     - Choose "External" (unless you have a Google Workspace)
     - Fill in the required fields (app name, support email)
     - Add your email as a test user
   - Select **Desktop app** as the application type
   - Give it a name (e.g., "Bedrock Runbook Reader")
   - Click **Create**

5. Download the credentials:
   - Click the download icon (⬇️) next to your newly created OAuth client
   - This downloads a JSON file with your `client_id` and `client_secret`

## Step 2: Get a Refresh Token

You need to complete the OAuth flow once to get a refresh token. The easiest way is using Google's OAuth 2.0 Playground:

### Using OAuth 2.0 Playground

1. Go to [OAuth 2.0 Playground](https://developers.google.com/oauthplayground/)

2. Configure to use your own credentials:
   - Click the **settings gear icon** (⚙️) in the top right
   - Check **"Use your own OAuth credentials"**
   - Enter your **OAuth Client ID** (from the JSON file you downloaded)
   - Enter your **OAuth Client secret** (from the JSON file you downloaded)

3. Authorize APIs:
   - In the left panel under "Step 1", scroll down to **"Google Docs API v1"**
   - Select: `https://www.googleapis.com/auth/documents.readonly`
   - Click **"Authorize APIs"** button
   - Sign in with your Google account
   - Grant the requested permissions

4. Exchange authorization code:
   - In "Step 2", click **"Exchange authorization code for tokens"**
   - Copy the **"Refresh token"** that appears
   - **Important**: Save this token securely - you won't be able to see it again!

## Step 3: Set Environment Variables

Now you have all three pieces of information you need:

```bash
# From the downloaded JSON file:
export GOOGLE_CLIENT_ID="123456789-abcdefg.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="GOCSPX-AbCdEfGhIjKlMnOpQrStUvWxYz"

# From OAuth 2.0 Playground:
export GOOGLE_REFRESH_TOKEN="1//0abcdefghijklmnopqrstuvwxyz..."

# Your Google Doc URL:
export GOOGLE_DOC_URL="https://docs.google.com/document/d/YOUR_DOC_ID/edit"
```

## Step 4: Verify Access to Your Google Doc

Make sure the Google account you used in the OAuth flow (Step 2) has access to the Google Doc:
- You must own the document, OR
- The document must be shared with you with at least "Viewer" access

## Alternative: Using gcloud CLI

If you prefer using the command line, you can use the gcloud CLI:

```bash
# Install gcloud if you haven't already
# https://cloud.google.com/sdk/docs/install

# Authenticate
gcloud auth application-default login --scopes=https://www.googleapis.com/auth/documents.readonly

# The credentials will be stored in:
# ~/.config/gcloud/application_default_credentials.json
```

Then extract the values from that file:
```bash
export GOOGLE_CLIENT_ID=$(jq -r '.client_id' ~/.config/gcloud/application_default_credentials.json)
export GOOGLE_CLIENT_SECRET=$(jq -r '.client_secret' ~/.config/gcloud/application_default_credentials.json)
export GOOGLE_REFRESH_TOKEN=$(jq -r '.refresh_token' ~/.config/gcloud/application_default_credentials.json)
```

## Security Notes

⚠️ **Keep your credentials secure**:
- Never commit `client_secret` or `refresh_token` to version control
- Don't share these values publicly
- Refresh tokens can be used to access your Google account resources
- You can revoke access at any time in [Google Account Settings](https://myaccount.google.com/permissions)

## Troubleshooting

### "You can't sign in because app sent an invalid request"

This error occurs when using OAuth 2.0 Playground with Desktop app credentials. **Desktop app credentials cannot have redirect URIs configured** - this is a Google Cloud limitation.

#### Solution 1: Use gcloud CLI Instead (Recommended for Desktop Apps)

Desktop app credentials work perfectly with the gcloud CLI method. This is the recommended approach:

```bash
# Authenticate with the specific scope
gcloud auth application-default login --scopes=https://www.googleapis.com/auth/documents.readonly

# The credentials will be stored in:
# ~/.config/gcloud/application_default_credentials.json
```

Then extract the values:
```bash
# Set the credentials file path
CREDS_FILE=~/.config/gcloud/application_default_credentials.json

# Extract with jq (recommended)
export GOOGLE_CLIENT_ID=$(jq -r '.client_id' $CREDS_FILE)
export GOOGLE_CLIENT_SECRET=$(jq -r '.client_secret' $CREDS_FILE)
export GOOGLE_REFRESH_TOKEN=$(jq -r '.refresh_token' $CREDS_FILE)
```

#### Solution 2: Create a Web Application OAuth Client for OAuth Playground

If you prefer to use OAuth Playground, create a separate **Web application** OAuth client:

1. Go to [Google Cloud Console Credentials](https://console.cloud.google.com/apis/credentials)
2. Click **+ CREATE CREDENTIALS** → **OAuth client ID**
3. Select **Web application** (NOT Desktop app!)
4. Name it (e.g., "OAuth Playground Web Client")
5. Under "Authorized redirect URIs", add: `https://developers.google.com/oauthplayground`
6. Click "Create"
7. Use this Web client's credentials in OAuth Playground

**Note**: You can use either the Desktop app or Web app credentials in your Scala application - both will work the same way once you have the refresh token.

### "This app isn't verified"
- This is normal for apps in testing mode
- Click "Advanced" → "Go to [App Name] (unsafe)"
- This only appears the first time you authorize

### "Access blocked: This app's request is invalid"
- Make sure you've enabled the Google Docs API in your project
- Check that you've configured the OAuth consent screen
- Verify the redirect URI matches what's expected

### Refresh token not showing in OAuth Playground
- Make sure you checked "Use your own OAuth credentials"
- Try revoking access and starting over: [Google Account Permissions](https://myaccount.google.com/permissions)
