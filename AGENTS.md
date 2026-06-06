# Automated Production Environment Verification

This document describes how automated agents or test scripts can verify the status of a production universe environment using a test account. 

## Environment Variables

To perform the verification, the following environment variables must be set:

| Variable | Description | Example Value |
|----------|-------------|---------------|
| `OWGE_AI_USERNAME` | The username or email of the test account. | `test_agent@owge.com` |
| `OWGE_AI_PASSWORD` | The password of the test account. | `securepassword123` |
| `OWGE_AI_KGDW_URL` | The base URL of the central account authentication server (KGDW). | `https://account.owge.com` |
| `OWGE_AI_KGDW_U` | The target universe ID to resolve and verify. | `0` |

---

## Step-by-Step Flow

### Step 1: Obtain Access Token (`/oauth/token`)
First, authenticate the test account using the OAuth2 password grant to retrieve a JWT token. Send a `POST` request to the account server's token endpoint:

* **Endpoint:** `POST $OWGE_AI_KGDW_URL/oauth/token`
* **Content-Type:** `application/x-www-form-urlencoded`
* **Request Parameters:**
  * `grant_type`: `password`
  * `client_id`: `1e39e154-8ec1-4c72-81ed-48b47a2a7dd2` (Default production Client ID, see [environment.prod.ts](file:///home/kevin/projects/owge/game-frontend/projects/game-frontend/src/environments/environment.prod.ts))
  * `client_secret`: `1234` (Default client secret, see [login.service.ts](file:///home/kevin/projects/owge/game-frontend/modules/owge-core/src/lib/services/login.service.ts))
  * `username`: `$OWGE_AI_USERNAME`
  * `password`: `$OWGE_AI_PASSWORD`

* **Response:**
  The server returns a JSON payload containing the JWT token under the `access_token` or `token` property.

---

### Step 2: Fetch Universe List (`/universe/findOfficials`)
Get the list of official, active universes registered with the account system:

* **Endpoint:** `GET $OWGE_AI_KGDW_URL/universe/findOfficials`
* **Response:** A JSON array of universe configurations (see [UniverseController.php](file:///home/kevin/projects/owge/mock_account/src/Controller/UniverseController.php)):
  ```json
  [
    {
      "id": 0,
      "name": "Beta Universe",
      "description": "Public test universe",
      "restBaseUrl": "https://api-beta.owge.com",
      "frontendUrl": "https://beta.owge.com"
    }
  ]
  ```

---

### Step 3: Resolve the Target Universe
1. Iterate over the array returned in **Step 2**.
2. Locate the universe object whose `id` matches `$OWGE_AI_KGDW_U`.
3. Extract the `restBaseUrl` property from that object.

---

### Step 4: Verify Universe Status (`/game/user/exists`)
Using the retrieved `restBaseUrl` and the JWT token from **Step 1**, verify the game server's status and check if the user profile exists within the universe.

> [!IMPORTANT]
> All endpoints starting with `/game` in a universe require the JWT bearer token for authorization.

* **Endpoint:** `GET <restBaseUrl>/game/user/exists`
* **Headers:**
  * `Authorization: Bearer <JWT_TOKEN>`
* **Response:**
  * A JSON boolean (`true` or `false`) indicating if the user has subscribed/registered within this universe.
  * Status code `200 OK` indicates the universe REST backend is healthy and responding.

---

## Actionable Bash Script Example

You can use the following script to automate the entire validation workflow:

```bash
#!/usr/bin/env bash
# verify_universe.sh
set -e

# 1. Verify required environment variables
if [ -z "$OWGE_AI_USERNAME" ] || [ -z "$OWGE_AI_PASSWORD" ] || [ -z "$OWGE_AI_KGDW_URL" ] || [ -z "$OWGE_AI_KGDW_U" ]; then
  echo "Error: Missing required environment variables."
  echo "Please set: OWGE_AI_USERNAME, OWGE_AI_PASSWORD, OWGE_AI_KGDW_URL, and OWGE_AI_KGDW_U"
  exit 1
fi

echo "Authenticating against account server ($OWGE_AI_KGDW_URL)..."

# 2. Authenticate and extract the JWT token
TOKEN_RESPONSE=$(curl -s -X POST "$OWGE_AI_KGDW_URL/oauth/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=1e39e154-8ec1-4c72-81ed-48b47a2a7dd2" \
  -d "client_secret=1234" \
  -d "username=$OWGE_AI_USERNAME" \
  -d "password=$OWGE_AI_PASSWORD")

JWT_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token // .token // empty')

if [ -z "$JWT_TOKEN" ] || [ "$JWT_TOKEN" = "null" ]; then
  echo "Error: Failed to obtain JWT token. Response: $TOKEN_RESPONSE"
  exit 1
fi

echo "Successfully authenticated. Fetching universe list..."

# 3. Retrieve available universes
UNIVERSES=$(curl -s "$OWGE_AI_KGDW_URL/universe/findOfficials")

# 4. Resolve the target universe's restBaseUrl
REST_BASE_URL=$(echo "$UNIVERSES" | jq -r --arg id "$OWGE_AI_KGDW_U" '.[] | select((.id | tostring) == ($id | tostring)) | .restBaseUrl')

if [ -z "$REST_BASE_URL" ] || [ "$REST_BASE_URL" = "null" ]; then
  echo "Error: Universe ID $OWGE_AI_KGDW_U not found in official universes."
  echo "Available universes:"
  echo "$UNIVERSES" | jq -r '.[] | "  - ID: \(.id), Name: \(.name)"'
  exit 1
fi

echo "Resolved Universe ID $OWGE_AI_KGDW_U to base URL: $REST_BASE_URL"
echo "Querying universe status endpoint /game/user/exists..."

# 5. Invoke the /game/user/exists endpoint with Bearer auth
EXISTS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$REST_BASE_URL/game/user/exists" \
  -H "Authorization: Bearer $JWT_TOKEN")

HTTP_STATUS=$(echo "$EXISTS_RESPONSE" | tail -n1)
BODY=$(echo "$EXISTS_RESPONSE" | sed '$d')

if [ "$HTTP_STATUS" -ne 200 ]; then
  echo "Error: Target universe returned status code $HTTP_STATUS"
  echo "Response body: $BODY"
  exit 1
fi

echo "Success! Response: $BODY (HTTP Status: $HTTP_STATUS)"
```
