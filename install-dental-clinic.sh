#!/bin/bash

# =========================================
# Dental Clinic Management System
# Linux Mint Installation Script
# =========================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="$PROJECT_DIR/dist/DentalClinicAppointment_ManagementSystem.jar"
ICON_FILE="$PROJECT_DIR/src/com/dentalclinic/resources/VantageLogo.png"
BACKUP_DIR="$HOME/backups/dental_clinic"
CONFIG_DIR="$HOME/.dental_clinic"
CONFIG_FILE="$CONFIG_DIR/db.properties"
LOG_FILE="$PROJECT_DIR/install.log"
DB_NAME="dental_clinic_db"
DB_USER="dental_app"
DB_PASSWORD="DentalClinic2024!"

# =========================================
# Helper Functions
# =========================================

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}→ $1${NC}"
}

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

check_previous_install() {
    print_header "Checking Previous Installation"
    
    if [ -f "$CONFIG_FILE" ]; then
        print_warning "Previous configuration found at: $CONFIG_FILE"
        read -p "Do you want to keep existing configuration? (y/n): " keep_config
        if [[ "$keep_config" =~ ^[Nn]$ ]]; then
            print_info "Backing up old config..."
            mv "$CONFIG_FILE" "$CONFIG_FILE.bak.$(date +%Y%m%d_%H%M%S)"
            print_success "Old config backed up"
        else
            print_success "Keeping existing configuration"
        fi
    fi
}

# =========================================
# 1. System Checks
# =========================================

check_system() {
    print_header "1. Checking System Requirements"
    
    # Check if running on Linux
    if [[ "$OSTYPE" != "linux-gnu"* ]]; then
        print_error "This script is designed for Linux only"
        exit 1
    fi
    print_success "Operating System: Linux"
    
    # Check Java
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -1)
        print_success "Java installed: $JAVA_VERSION"
    else
        print_warning "Java not found. Installing OpenJDK 17..."
        sudo apt update
        sudo apt install -y openjdk-17-jre openjdk-17-jdk
        print_success "Java installed"
    fi
    
    # Check JAR file
    if [ -f "$JAR_FILE" ]; then
        print_success "JAR file found: $JAR_FILE"
    else
        print_error "JAR file not found at: $JAR_FILE"
        print_info "Please build the project first in NetBeans (Run -> Clean and Build Project)"
        exit 1
    fi
    
    # Check icon
    if [ -f "$ICON_FILE" ]; then
        print_success "Icon file found"
    else
        print_warning "Icon file not found. Using default icon."
    fi
}

# =========================================
# 2. Install MySQL
# =========================================

install_mysql() {
    print_header "2. Installing MySQL Server"
    
    if command -v mysql &> /dev/null; then
        print_success "MySQL already installed"
        MYSQL_VERSION=$(mysql --version)
        print_info "Version: $MYSQL_VERSION"
    else
        print_info "Installing MySQL Server..."
        sudo apt update
        sudo apt install -y mysql-server
        
        # Start MySQL
        sudo systemctl start mysql
        sudo systemctl enable mysql
        print_success "MySQL installed and started"
    fi
    
    # Check if MySQL is running
    if sudo systemctl is-active --quiet mysql; then
        print_success "MySQL is running"
    else
        print_warning "MySQL not running. Starting..."
        sudo systemctl start mysql
        print_success "MySQL started"
    fi
}

# =========================================
# 3. Setup Database
# =========================================

setup_database() {
    print_header "3. Setting up Database"
    
    # Find SQL schema file
    SQL_FILE=""
    if [ -f "$PROJECT_DIR/latest.sql" ]; then
        SQL_FILE="$PROJECT_DIR/latest.sql"
    elif [ -f "$PROJECT_DIR/dental_clinic_db.sql" ]; then
        SQL_FILE="$PROJECT_DIR/dental_clinic_db.sql"
    else
        print_warning "SQL schema file not found. You'll need to import it manually."
        read -p "Do you have the SQL file? Enter path or press Enter to skip: " sql_path
        if [ -n "$sql_path" ] && [ -f "$sql_path" ]; then
            SQL_FILE="$sql_path"
        fi
    fi
    
    # Ask for MySQL root password
    echo ""
    print_info "Please enter your MySQL root password (if set)"
    read -sp "MySQL root password (press Enter if none): " MYSQL_ROOT_PASS
    echo ""
    
    # Test connection
    if [ -z "$MYSQL_ROOT_PASS" ]; then
        if mysql -u root -e "SELECT 1" &> /dev/null; then
            print_success "Connected to MySQL as root (no password)"
            MYSQL_CMD="mysql -u root"
            MYSQL_ADMIN_CMD="mysqladmin -u root"
        else
            print_error "Cannot connect to MySQL as root"
            exit 1
        fi
    else
        if mysql -u root -p"$MYSQL_ROOT_PASS" -e "SELECT 1" &> /dev/null; then
            print_success "Connected to MySQL as root"
            MYSQL_CMD="mysql -u root -p\"$MYSQL_ROOT_PASS\""
            MYSQL_ADMIN_CMD="mysqladmin -u root -p\"$MYSQL_ROOT_PASS\""
        else
            print_error "Invalid MySQL root password"
            exit 1
        fi
    fi
    
    # Create database if not exists
    print_info "Creating database if not exists..."
    eval "$MYSQL_CMD -e \"CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\""
    print_success "Database '$DB_NAME' ready"
    
    # Import schema if provided
    if [ -n "$SQL_FILE" ] && [ -f "$SQL_FILE" ]; then
        print_info "Importing database schema from: $SQL_FILE"
        eval "$MYSQL_CMD $DB_NAME < \"$SQL_FILE\""
        print_success "Schema imported"
    else
        print_warning "No schema file imported. Database is empty."
    fi
    
    # Create application user
    print_info "Creating application user '$DB_USER'..."
    eval "$MYSQL_CMD -e \"CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASSWORD';\""
    eval "$MYSQL_CMD -e \"GRANT SELECT, INSERT, UPDATE, DELETE ON $DB_NAME.* TO '$DB_USER'@'localhost';\""
    eval "$MYSQL_CMD -e \"FLUSH PRIVILEGES;\""
    print_success "Application user created with limited privileges"
    
    # Add force_password_reset columns if not exists
    print_info "Updating schema with security columns..."
    eval "$MYSQL_CMD $DB_NAME -e \"ALTER TABLE patients ADD COLUMN IF NOT EXISTS force_password_reset TINYINT(1) DEFAULT 0;\" 2>/dev/null || true"
    eval "$MYSQL_CMD $DB_NAME -e \"ALTER TABLE staff ADD COLUMN IF NOT EXISTS force_password_reset TINYINT(1) DEFAULT 0;\" 2>/dev/null || true"
    eval "$MYSQL_CMD $DB_NAME -e \"ALTER TABLE patients ADD COLUMN IF NOT EXISTS failed_login_attempts INT DEFAULT 0;\" 2>/dev/null || true"
    eval "$MYSQL_CMD $DB_NAME -e \"ALTER TABLE staff ADD COLUMN IF NOT EXISTS failed_login_attempts INT DEFAULT 0;\" 2>/dev/null || true"
    eval "$MYSQL_CMD $DB_NAME -e \"ALTER TABLE patients ADD COLUMN IF NOT EXISTS account_locked TINYINT(1) DEFAULT 0;\" 2>/dev/null || true"
    eval "$MYSQL_CMD $DB_NAME -e \"ALTER TABLE staff ADD COLUMN IF NOT EXISTS account_locked TINYINT(1) DEFAULT 0;\" 2>/dev/null || true"
    eval "$MYSQL_CMD $DB_NAME -e \"ALTER TABLE patients ADD COLUMN IF NOT EXISTS lockout_time DATETIME DEFAULT NULL;\" 2>/dev/null || true"
    eval "$MYSQL_CMD $DB_NAME -e \"ALTER TABLE staff ADD COLUMN IF NOT EXISTS lockout_time DATETIME DEFAULT NULL;\" 2>/dev/null || true"
    print_success "Security columns added"
}

# =========================================
# 4. Create Configuration File
# =========================================

create_config() {
    print_header "4. Creating Configuration File"
    
    # Create config directory
    mkdir -p "$CONFIG_DIR"
    
    # Check if config exists and user wants to keep
    if [ -f "$CONFIG_FILE" ]; then
        print_info "Config file already exists at: $CONFIG_FILE"
        read -p "Overwrite? (y/n): " overwrite
        if [[ ! "$overwrite" =~ ^[Yy]$ ]]; then
            print_success "Keeping existing configuration"
            return
        fi
    fi
    
    # Create config file
    cat > "$CONFIG_FILE" << EOF
# Dental Clinic Database Configuration
# Created: $(date)
# DO NOT SHARE THIS FILE

db.url=jdbc:mysql://localhost:3306/$DB_NAME?useSSL=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8
db.user=$DB_USER
db.password=$DB_PASSWORD
EOF
    
    # Set secure permissions
    chmod 600 "$CONFIG_FILE"
    print_success "Configuration created at: $CONFIG_FILE"
    print_warning "File permissions set to 600 (readable only by you)"
}

# =========================================
# 5. Create Desktop Launcher
# =========================================

create_launcher() {
    print_header "5. Creating Desktop Launcher"
    
    # Create .desktop file
    DESKTOP_FILE="$HOME/.local/share/applications/dental-clinic.desktop"
    
    # Create directory if not exists
    mkdir -p "$HOME/.local/share/applications"
    
    cat > "$DESKTOP_FILE" << EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=Dental Clinic
Comment=Dental Clinic Appointment Management System
Exec=java -jar "$JAR_FILE"
Icon=$ICON_FILE
Terminal=false
StartupNotify=true
Categories=Office;Health;Medical;
Keywords=dental;clinic;appointment;health;
EOF
    
    chmod +x "$DESKTOP_FILE"
    print_success "Desktop launcher created at: $DESKTOP_FILE"
    
    # Also create on desktop if requested
    read -p "Create launcher on desktop as well? (y/n): " create_desktop
    if [[ "$create_desktop" =~ ^[Yy]$ ]]; then
        cp "$DESKTOP_FILE" "$HOME/Desktop/dental-clinic.desktop"
        chmod +x "$HOME/Desktop/dental-clinic.desktop"
        print_success "Desktop icon created"
    fi
}

# =========================================
# 6. Create Backup Script
# =========================================

create_backup_script() {
    print_header "6. Creating Backup System"
    
    BACKUP_SCRIPT="$HOME/bin/backup-dental-clinic.sh"
    mkdir -p "$HOME/bin"
    
    cat > "$BACKUP_SCRIPT" << EOF
#!/bin/bash
# Dental Clinic Database Backup Script

BACKUP_DIR="$BACKUP_DIR"
DATE=\$(date +%Y%m%d_%H%M%S)
DB_NAME="$DB_NAME"
DB_USER="$DB_USER"
DB_PASSWORD="$DB_PASSWORD"
KEEP_DAYS=30

mkdir -p "\$BACKUP_DIR"

# Perform backup
mysqldump -u "\$DB_USER" -p"\$DB_PASSWORD" "\$DB_NAME" > "\$BACKUP_DIR/backup_\$DATE.sql"

if [ \$? -eq 0 ]; then
    echo "[OK] Backup created: backup_\$DATE.sql"
    # Compress
    gzip "\$BACKUP_DIR/backup_\$DATE.sql"
    echo "[OK] Backup compressed"
    
    # Remove old backups
    find "\$BACKUP_DIR" -name "backup_*.sql.gz" -mtime +\$KEEP_DAYS -delete
    echo "[OK] Removed backups older than \$KEEP_DAYS days"
else
    echo "[ERROR] Backup failed!"
    exit 1
fi
EOF
    
    chmod +x "$BACKUP_SCRIPT"
    print_success "Backup script created at: $BACKUP_SCRIPT"
    
    # Ask about setting up cron job
    read -p "Set up automatic daily backup at 2 AM? (y/n): " setup_cron
    if [[ "$setup_cron" =~ ^[Yy]$ ]]; then
        (crontab -l 2>/dev/null; echo "0 2 * * * $BACKUP_SCRIPT") | crontab -
        print_success "Daily backup scheduled at 2:00 AM"
    fi
}

# =========================================
# 7. Create Uninstall Script
# =========================================

create_uninstall_script() {
    print_header "7. Creating Uninstall Script"
    
    UNINSTALL_SCRIPT="$PROJECT_DIR/uninstall.sh"
    
    cat > "$UNINSTALL_SCRIPT" << EOF
#!/bin/bash
# Dental Clinic Uninstall Script

echo "========================================="
echo "Dental Clinic - Uninstall"
echo "========================================="
echo ""

read -p "Remove configuration files? (y/n): " remove_config
if [[ "\$remove_config" =~ ^[Yy]$ ]]; then
    rm -rf "$CONFIG_DIR"
    echo "✓ Removed configuration"
fi

read -p "Remove desktop launcher? (y/n): " remove_launcher
if [[ "\$remove_launcher" =~ ^[Yy]$ ]]; then
    rm -f "$HOME/.local/share/applications/dental-clinic.desktop"
    rm -f "$HOME/Desktop/dental-clinic.desktop"
    echo "✓ Removed launcher"
fi

read -p "Remove backup directory? (y/n): " remove_backup
if [[ "\$remove_backup" =~ ^[Yy]$ ]]; then
    rm -rf "$BACKUP_DIR"
    echo "✓ Removed backups"
fi

read -p "Drop database? (y/n): " drop_db
if [[ "\$drop_db" =~ ^[Yy]$ ]]; then
    read -sp "MySQL root password: " root_pass
    echo ""
    if [ -z "\$root_pass" ]; then
        mysql -u root -e "DROP DATABASE IF EXISTS $DB_NAME; DROP USER IF EXISTS '$DB_USER'@'localhost';"
    else
        mysql -u root -p"\$root_pass" -e "DROP DATABASE IF EXISTS $DB_NAME; DROP USER IF EXISTS '$DB_USER'@'localhost';"
    fi
    echo "✓ Database removed"
fi

echo ""
echo "Uninstall complete!"
EOF
    
    chmod +x "$UNINSTALL_SCRIPT"
    print_success "Uninstall script created at: $UNINSTALL_SCRIPT"
}

# =========================================
# 8. Test Application
# =========================================

test_application() {
    print_header "8. Testing Application"
    
    print_info "Testing database connection..."
    if java -jar "$JAR_FILE" --test-connection 2>/dev/null; then
        print_success "Database connection successful"
    else
        print_warning "Manual test required. Run the app to verify."
    fi
    
    print_info "You can now run the application with:"
    echo ""
    echo -e "${GREEN}   java -jar \"$JAR_FILE\"${NC}"
    echo ""
    echo "Or use the desktop launcher from your application menu."
}

# =========================================
# 9. Installation Summary
# =========================================

print_summary() {
    print_header "Installation Complete!"
    
    echo -e "${GREEN}✓ MySQL Server installed and running${NC}"
    echo -e "${GREEN}✓ Database '$DB_NAME' created${NC}"
    echo -e "${GREEN}✓ Application user '$DB_USER' created${NC}"
    echo -e "${GREEN}✓ Configuration file created at: $CONFIG_FILE${NC}"
    echo -e "${GREEN}✓ Desktop launcher created${NC}"
    echo -e "${GREEN}✓ Backup script created at: $BACKUP_SCRIPT${NC}"
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Next Steps:${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo "1. Run the application:"
    echo "   java -jar \"$JAR_FILE\""
    echo ""
    echo "2. Or find 'Dental Clinic' in your application menu"
    echo ""
    echo "3. Default login credentials (if you imported schema):"
    echo "   Patient: username 'alberto', password 'pass123'"
    echo "   Admin: username 'superAdmin', password 'pass123'"
    echo ""
    echo "4. To backup database: $BACKUP_SCRIPT"
    echo ""
    echo "5. To uninstall: $UNINSTALL_SCRIPT"
    echo ""
    echo -e "${GREEN}Installation log saved to: $LOG_FILE${NC}"
    echo ""
}

# =========================================
# Main Execution
# =========================================

main() {
    clear
    echo -e "${BLUE}"
    echo "╔═══════════════════════════════════════════╗"
    echo "║     Dental Clinic Management System       ║"
    echo "║         Linux Mint Installer               ║"
    echo "╚═══════════════════════════════════════════╝"
    echo -e "${NC}"
    
    # Run installation steps
    check_previous_install
    check_system
    install_mysql
    setup_database
    create_config
    create_launcher
    create_backup_script
    create_uninstall_script
    test_application
    print_summary
    
    log "Installation completed successfully"
}

# Run main function
main
