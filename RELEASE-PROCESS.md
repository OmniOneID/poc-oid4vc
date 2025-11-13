# Release Process

This document describes the Release Process for deployment of feature additions and modifications for each repository. It covers version management and release procedures.

## 1. Versioning

The project follows a versioning rule in the format "X.Y.Z" where:
- X (Major): Significant changes that are not backward compatible
- Y (Minor): New features that are backward compatible
- Z (Patch): Bug fixes or minor improvements that are backward compatible

When the Major version is updated, both Minor and Patch are reset to 0.
<br>
When the Minor version is updated, the Patch is reset to 0.

##  2. Release Procedure
Repository is managed independently, following these steps:

1. **Change Log Review**  
  Review the  for each module to ensure all changes are recorded.

2. **Create a Release Branch**  
  If there are changes or modifications, create a branch "release/VX.Y.Z" for the release.
- Example: If there are bug fixes or minor improvements for V1.0.0, create a branch "release/V1.0.1".

  For modules without changes, use the existing version (V1.0.0) and the already distributed JAR or library.

3. **Merge into Main and Develop Branches**    
  - Merge the Release branch (release/VX.Y.Z) into both main and develop branches.

4. **Create a Release for Each Repository**. 
- When the branch is merged into main, trigger the  using GitHub Actions to create the Release and perform version tagging. The generated  includes the following:
   - Version name
   - Summary of the changelog
   - Source code archive
   - Distributed files
- Delete the release/VX.Y.Z branch after the release.