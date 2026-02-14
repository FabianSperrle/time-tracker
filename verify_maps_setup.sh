#!/bin/bash

# Verification script for Google Maps API key configuration
# Run this script to check if your Maps API key is properly configured

set -e

echo "======================================"
echo "Google Maps API Key Verification"
echo "======================================"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if local.properties exists
echo "1. Checking local.properties file..."
if [ -f "local.properties" ]; then
    echo -e "${GREEN}✓${NC} local.properties exists"

    # Check if it contains MAPS_API_KEY
    if grep -q "MAPS_API_KEY" local.properties; then
        echo -e "${GREEN}✓${NC} MAPS_API_KEY found in local.properties"

        # Extract the key value
        KEY_VALUE=$(grep "MAPS_API_KEY" local.properties | cut -d'=' -f2 | tr -d ' ')

        if [ "$KEY_VALUE" = "YOUR_API_KEY_HERE" ]; then
            echo -e "${RED}✗${NC} API key is still the placeholder 'YOUR_API_KEY_HERE'"
            echo -e "${YELLOW}  Action required: Replace with your actual API key${NC}"
            echo ""
            echo "  Get your API key from: https://console.cloud.google.com/"
            echo "  See docs/GOOGLE_MAPS_SETUP.md for detailed instructions"
            exit 1
        elif [ -z "$KEY_VALUE" ]; then
            echo -e "${RED}✗${NC} API key value is empty"
            echo -e "${YELLOW}  Action required: Set your API key in local.properties${NC}"
            exit 1
        else
            # Check if it looks like a valid Google API key (starts with AIza)
            if [[ $KEY_VALUE == AIza* ]]; then
                echo -e "${GREEN}✓${NC} API key format looks valid (starts with 'AIza')"
                echo -e "${GREEN}✓${NC} Configuration appears correct!"
                echo ""
                echo "Next steps:"
                echo "  1. Rebuild the project: ./gradlew clean assembleDebug"
                echo "  2. Install on device: ./gradlew installDebug"
                echo "  3. Test the map screen in the app"
                exit 0
            else
                echo -e "${YELLOW}⚠${NC} API key doesn't start with 'AIza' (expected format)"
                echo "  This might be a valid key, but Google Maps API keys typically start with 'AIza'"
                echo "  If you continue to have issues, verify the key in Google Cloud Console"
                exit 0
            fi
        fi
    else
        echo -e "${RED}✗${NC} MAPS_API_KEY not found in local.properties"
        echo -e "${YELLOW}  Action required: Add 'MAPS_API_KEY=your_key_here' to local.properties${NC}"
        exit 1
    fi
else
    echo -e "${RED}✗${NC} local.properties file not found"
    echo ""
    echo "Action required:"
    echo "  1. Copy the template: cp local.properties.template local.properties"
    echo "  2. Edit local.properties and add your API key"
    echo "  3. See docs/GOOGLE_MAPS_SETUP.md for detailed instructions"
    exit 1
fi

# Check environment variable (optional)
echo ""
echo "2. Checking MAPS_API_KEY environment variable (optional)..."
if [ -n "$MAPS_API_KEY" ]; then
    echo -e "${GREEN}✓${NC} MAPS_API_KEY environment variable is set"
    echo "  Note: local.properties takes precedence over environment variable"
else
    echo -e "${YELLOW}○${NC} MAPS_API_KEY environment variable not set (this is OK)"
    echo "  Using local.properties instead"
fi

echo ""
echo "======================================"
echo "Verification Complete"
echo "======================================"
