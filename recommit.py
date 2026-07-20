import os
import subprocess
import shlex

def run(cmd):
    subprocess.run(cmd, shell=True, check=True)

def get_commit_msg(filepath):
    filename = os.path.basename(filepath)
    ext = os.path.splitext(filename)[1].lower()
    name = os.path.splitext(filename)[0]
    
    if ext == '.kt':
        if 'Screen' in name:
            return f"Implement {name} UI component"
        if 'ViewModel' in name:
            return f"Add {name} for state management"
        if 'Dao' in name or 'Database' in name:
            return f"Configure local database with {name}"
        if 'Repository' in name:
            return f"Add data repository {name}"
        if 'Item' in name or 'State' in name:
            return f"Define {name} data structure"
        return f"Add {filename} utility or component"
    if ext == '.xml':
        if 'strings' in name:
            return "Define string resources"
        if 'colors' in name:
            return "Define color resources"
        if 'themes' in name:
            return "Configure app themes"
        if 'AndroidManifest' in name:
            return "Configure AndroidManifest.xml settings"
        if 'ic_launcher' in name:
            return f"Add {filename} icon asset"
        return f"Add {filename} configuration"
    if ext == '.kts' or ext == '.properties':
        return f"Configure build settings in {filename}"
    if ext == '.png' or ext == '.webp':
        return f"Add graphic asset {filename}"
    return f"Add {filename}"

def main():
    # Delete the branch history, making it a fresh repo without deleting files
    run('git update-ref -d HEAD')
    # Remove everything from the git index
    run('git rm -r --cached . || true')
    
    # Add standard files first
    run('git add .gitignore')
    run('git commit -m "Add .gitignore configuration"')
    
    # Get all untracked files that aren't ignored
    result = subprocess.run('git ls-files --others --exclude-standard', shell=True, capture_output=True, text=True)
    files = result.stdout.strip().split('\n')
    
    for f in files:
        if not f:
            continue
        msg = get_commit_msg(f)
        # Avoid shell injection
        safe_f = shlex.quote(f)
        safe_msg = shlex.quote(msg)
        run(f'git add {safe_f}')
        run(f'git commit -m {safe_msg}')
        
if __name__ == "__main__":
    main()
