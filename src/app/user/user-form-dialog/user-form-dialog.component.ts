import {Component, Inject} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {User} from "../../models/User";

@Component({
  selector: 'app-user-form-dialog',
  templateUrl: './user-form-dialog.component.html',
  styleUrls: ['./user-form-dialog.component.scss']
})
export class UserFormDialogComponent {
  userForm: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<UserFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { user: User | null },
    private fb: FormBuilder
  ) {
    this.userForm = this.fb.group({
      username: [data.user?.firstname || '', Validators.required],
      email: [data.user?.email || '', [Validators.required, Validators.email]],
      password: ['', data.user ? [] : [Validators.required]], // only required on create
      role: [data.user?.role?.nom || '', Validators.required]
    });
  }

  save(): void {
    const user: User = {
      ...this.data.user,
      ...this.userForm.value
    };
    this.dialogRef.close(user);
  }

  cancel(): void {
    this.dialogRef.close();

}
}
