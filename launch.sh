#!/bin/bash

# Dental Clinic Launch Script with MySQL Management

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to show header
show_header() {
    clear
    echo -e "${BLUE}=========================================${NC}"
    echo -e "${BLUE}   Dental Clinic Management System${NC}"
    echo -e "${BLUE}=========================================${NC}"
    echo ""
}

# Function to stop MySQL on exit
cleanup() {
    echo ""
    echo -e "${YELLOW}Shutting down...${NC}"
    echo -e "${YELLOW}Stopping MySQL...${NC}"
    sudo /opt/lampp/lampp stopmysql
    echo -e "${GREEN}✓ MySQL stopped. Goodbye!${NC}"
    exit 0
}

# Set trap to catch exit signals
trap cleanup EXIT INT TERM

# Show header
show_header

# Check if MySQL is running
echo -e "${YELLOW}Checking MySQL status...${NC}"
if sudo /opt/lampp/lampp status | grep -q "MySQL is not running"; then
    echo -e "${YELLOW}Starting MySQL...${NC}"
    sudo /opt/lampp/lampp startmysql
    sleep 2
    echo -e "${GREEN}✓ MySQL started${NC}"
else
    echo -e "${GREEN}✓ MySQL is already running${NC}"
fi

echo ""

# Launch the app
echo -e "${YELLOW}Starting Dental Clinic application...${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""

cd ~/Applications/DentalClinicSystem/dist
java -jar DentalClinicAppointment_ManagementSystem.jar

# The cleanup function will run when the app exits
