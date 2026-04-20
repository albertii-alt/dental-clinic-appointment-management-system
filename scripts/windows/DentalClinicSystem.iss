#define MyAppName "Dental Clinic System"
#ifndef MyAppVersion
  #define MyAppVersion "1.0.0"
#endif
#define MyAppPublisher "Dental Clinic Team"
#define MyAppExeName "run-dental-clinic.bat"

[Setup]
AppId={{B28C88D2-E6F0-4A4D-8E2A-8B3FCF8CB8E6}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={autopf}\DentalClinicSystem
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
OutputBaseFilename=DentalClinicSystem-Setup-{#MyAppVersion}
SetupIconFile=app-icon.ico
UninstallDisplayIcon={app}\app-icon.ico
Compression=lzma
SolidCompression=yes
WizardStyle=modern
ArchitecturesInstallIn64BitMode=x64compatible

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional icons:"; Flags: unchecked

[Files]
Source: "..\..\dist\DentalClinicAppointment_ManagementSystem.jar"; DestDir: "{app}\app"; Flags: ignoreversion
Source: "..\..\dist\lib\*"; DestDir: "{app}\app\lib"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\..\backend-spring\target\backend-spring-0.1.0.jar"; DestDir: "{app}\backend"; Flags: ignoreversion
Source: "..\..\build\windows-runtime\runtime\*"; DestDir: "{app}\runtime"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "app-icon.ico"; DestDir: "{app}"; DestName: "app-icon.ico"; Flags: ignoreversion
Source: "run-dental-clinic.bat"; DestDir: "{app}"; DestName: "run-dental-clinic.bat"; Flags: ignoreversion
Source: "db.properties.template"; DestDir: "{app}"; DestName: "db.properties.template"; Flags: ignoreversion

[Dirs]
Name: "{userdocs}\..\.dental_clinic"; Flags: uninsneveruninstall

[Code]
procedure CopyDbPropertiesIfMissing();
var
  DestDir, DestFile, SrcFile: String;
begin
  DestDir := ExpandConstant('{userdocs}') + '\..\.dental_clinic';
  DestFile := DestDir + '\db.properties';
  SrcFile  := ExpandConstant('{app}') + '\db.properties.template';

  if not DirExists(DestDir) then
    CreateDir(DestDir);

  if not FileExists(DestFile) then
    FileCopy(SrcFile, DestFile, False);
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
    CopyDbPropertiesIfMissing();
end;

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\app-icon.ico"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\app-icon.ico"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "Launch {#MyAppName}"; Flags: nowait postinstall skipifsilent
