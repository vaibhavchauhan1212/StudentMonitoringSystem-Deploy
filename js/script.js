let currentSessionUser = null;
let distributionChartInstance = null;

function switchView(targetViewName) {
    document.querySelectorAll('.portal-view').forEach(view => view.style.display = 'none');
    document.querySelectorAll('.nav-menu a').forEach(tab => tab.classList.remove('active'));
    const viewNode = document.getElementById('view' + targetViewName.charAt(0).toUpperCase() + targetViewName.slice(1));
    if (viewNode) viewNode.style.display = 'block';
}

function openModal(modalId) {
    if (['markAttendanceModal', 'performanceModal', 'assignmentModal', 'enrollStudentModal'].includes(modalId)) {
        populateSubjectDropdownMenus();
    }
    document.getElementById(modalId).style.display = 'flex';
}

function closeModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
}

// NEW MODULE: TOGGLES LABELS DYNAMICALLY FOR THE MASTER ADMINISTRATIVE SECURITY PANEL
function toggleSecurityInputs() {
    const type = document.getElementById('secActionType').value;
    const label = document.getElementById('secDynamicLabel');
    const input = document.getElementById('secValueInput');
    
    if (type === 'UPDATE_PASSWORD') {
        label.innerText = "New Password String";
        input.placeholder = "••••••••";
    } else {
        label.innerText = "Desired New Primary Account ID Code Tag";
        input.placeholder = "e.g., STU999";
    }
}

// NEW MODULE: DISPATCHES MASTER ADMINISTRATIVE CREDENTIAL OVERWRITE CORRECTIONS
document.getElementById('masterSecurityForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const params = `action=MASTER_SECURITY_OVERRIDE&targetId=${encodeURIComponent(document.getElementById('secTargetId').value)}` +
                   `&type=${document.getElementById('secActionType').value}` +
                   `&newValue=${encodeURIComponent(document.getElementById('secValueInput').value)}`;
    executePostTransaction('/admin/users', params, 'masterSecurityModal', loadAdminWorkspaceMetrics);
});

function openSubjectModal() {
    fetch('/admin/users?fetchMode=FACULTY_ONLY')
    .then(res => res.json())
    .then(facultyList => {
        const selectNode = document.getElementById('subFacultySelect');
        selectNode.innerHTML = '<option value="NONE">-- Leave Unassigned --</option>';
        facultyList.forEach(fac => {
            selectNode.innerHTML += `<option value="${fac.id}">${fac.name} (${fac.id})</option>`;
        });
        openModal('subjectModal');
    });
}

function openTimetableModal() {
    fetch('/admin/users?fetchMode=ALL_SUBJECTS')
    .then(res => res.json())
    .then(subjects => {
        const selectNode = document.getElementById('ttSubjectSelect');
        selectNode.innerHTML = '';
        subjects.forEach(sub => {
            selectNode.innerHTML += `<option value="${sub.code}">${sub.name} (${sub.code})</option>`;
        });
        openModal('timetableModal');
    });
}

function openAttendanceModal() {
    fetch('/admin/users?fetchMode=ALL_SUBJECTS')
    .then(res => res.json())
    .then(subjects => {
        const selectNode = document.getElementById('attSubjectSelect');
        selectNode.innerHTML = '<option value="">-- Choose Subject --</option>';
        subjects.forEach(sub => {
            selectNode.innerHTML += `<option value="${sub.code}">${sub.name} (${sub.code})</option>`;
        });
        document.getElementById('attStudentSelect').innerHTML = '<option value="">-- Choose Subject First --</option>';
        document.getElementById('markAttendanceModal').style.display = 'flex';
    });
}

function fetchEnrolledStudentsForAttendance() {
    const subjectCode = document.getElementById('attSubjectSelect').value;
    if (!subjectCode) return;

    fetch(`/students?fetchMode=ENROLLED_STUDENTS_LIST&subjectCode=${encodeURIComponent(subjectCode)}`)
    .then(res => res.json())
    .then(students => {
        const studentSelect = document.getElementById('attStudentSelect');
        studentSelect.innerHTML = '';
        if (students.length === 0) {
            studentSelect.innerHTML = '<option value="">No students enrolled in this subject</option>';
        } else {
            students.forEach(st => {
                studentSelect.innerHTML += `<option value="${st.id}">${st.name} (#${st.id})</option>`;
            });
        }
    });
}

function populateSubjectDropdownMenus() {
    fetch('/admin/users?fetchMode=ALL_SUBJECTS')
    .then(res => res.json())
    .then(subjects => {
        const targetIds = ['enrollSubjectSelect', 'perfSubjectSelect', 'assignSubjectSelect'];
        targetIds.forEach(id => {
            const el = document.getElementById(id);
            if (el) {
                el.innerHTML = '';
                subjects.forEach(sub => {
                    el.innerHTML += `<option value="${sub.code}">${sub.name} (${sub.code})</option>`;
                });
            }
        });
    });
}

document.getElementById('mapParentForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const params = `action=MAP_PARENT_STUDENT&parentId=${encodeURIComponent(document.getElementById('mapParentId').value)}` +
                   `&studentId=${encodeURIComponent(document.getElementById('mapStudentId').value)}`;
    executePostTransaction('/admin/users', params, 'mapParentModal', loadAdminWorkspaceMetrics);
});

document.getElementById('facultyBehaviorForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const params = `action=MANAGE_BEHAVIOR&studentId=${encodeURIComponent(document.getElementById('behStudentId').value)}` +
                   `&type=${document.getElementById('behActionType').value}` +
                   `&points=${document.getElementById('behPointsValue').value}`;
    executePostTransaction('/students', params, 'facultyBehaviorModal', loadFacultyWorkspaceMetrics);
});

function updateLeaveRequestStatus(leaveId, decisionString) {
    const params = `action=PROCESS_LEAVE_DECISION&leaveId=${leaveId}&decision=${decisionString}`;
    fetch('/students', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
    })
    .then(res => {
        if (!res.ok) throw new Error("Leave state modification rejected.");
        alert("Leave status recorded as " + decisionString);
        loadFacultyWorkspaceMetrics();
    })
    .catch(err => alert(err.message));
}

document.getElementById('payFeeForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const params = `action=PROCESS_FEE_PAYMENT&studentId=${encodeURIComponent(currentSessionUser.id)}` +
                   `&amount=${document.getElementById('payFeeAmount').value}`;
    executePostTransaction('/students', params, 'payFeeModal', loadStudentWorkspaceMetrics);
});

document.getElementById('enrollStudentForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const params = `action=ENROLL_STUDENT&studentId=${encodeURIComponent(document.getElementById('enrollStudentId').value)}` +
                   `&subjectCode=${encodeURIComponent(document.getElementById('enrollSubjectSelect').value)}`;
    executePostTransaction('/students', params, 'enrollStudentModal', loadFacultyWorkspaceMetrics);
});

document.getElementById('subjectForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const params = `action=CREATE_SUBJECT&code=${encodeURIComponent(document.getElementById('subCode').value)}` +
                   `&name=${encodeURIComponent(document.getElementById('subTitle').value)}` +
                   `&facultyId=${encodeURIComponent(document.getElementById('subFacultySelect').value)}`;
    executePostTransaction('/admin/users', params, 'subjectModal', loadAdminWorkspaceMetrics);
});

document.getElementById('loginForm').addEventListener('submit', function(event) {
    event.preventDefault();
    const id = document.getElementById('loginId').value;
    const password = document.getElementById('loginPassword').value;

    fetch('/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `id=${encodeURIComponent(id)}&password=${encodeURIComponent(password)}`
    })
    .then(res => { if (!res.ok) throw new Error("Authentication failure."); return res.json(); })
    .then(session => {
        currentSessionUser = session;
        document.getElementById('loginSection').style.display = 'none';
        document.getElementById('portalSection').style.display = 'flex';
        document.getElementById('userBadge').innerText = session.role;
        document.querySelectorAll('.role-nav').forEach(nav => nav.style.display = 'none');
        
        if (session.role === 'ADMIN') { document.getElementById('navAdmin').style.display = 'flex'; switchView('adminDashboard'); loadAdminWorkspaceMetrics(); }
        else if (session.role === 'FACULTY') { document.getElementById('navFaculty').style.display = 'flex'; switchView('facultyDashboard'); loadFacultyWorkspaceMetrics(); }
        else if (session.role === 'STUDENT') { document.getElementById('navStudent').style.display = 'flex'; switchView('studentDashboard'); loadStudentWorkspaceMetrics(); }
        else if (session.role === 'PARENT') { document.getElementById('navParent').style.display = 'flex'; switchView('parentDashboard'); loadParentWorkspaceMetrics(); }
    })
    .catch(err => alert(err.message));
});

document.getElementById('provisionForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const params = `action=PROVISION&id=${encodeURIComponent(document.getElementById('pId').value)}` +
                   `&password=${encodeURIComponent(document.getElementById('pPassword').value)}` +
                   `&role=${document.getElementById('pRole').value}` +
                   `&name=${encodeURIComponent(document.getElementById('pName').value)}` +
                   `&email=${encodeURIComponent(document.getElementById('pEmail').value)}`;
    executePostTransaction('/admin/users', params, 'provisionModal', loadAdminWorkspaceMetrics);
});

document.getElementById('attendanceForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const params = `action=MARK_ATTENDANCE&studentId=${encodeURIComponent(document.getElementById('attStudentSelect').value)}` +
                   `&subjectCode=${encodeURIComponent(document.getElementById('attSubjectSelect').value)}` +
                   `&status=${document.getElementById('attStatus').value}`;
    executePostTransaction('/students', params, 'markAttendanceModal', loadFacultyWorkspaceMetrics);
});

document.getElementById('performanceForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const params = `action=UPDATE_PERFORMANCE&studentId=${encodeURIComponent(document.getElementById('perfStudentId').value)}` +
                   `&subjectCode=${encodeURIComponent(document.getElementById('perfSubjectSelect').value)}` +
                   `&internal=${document.getElementById('perfInternal').value}` +
                   `&assignment=${document.getElementById('perfAssignment').value}` +
                   `&semester=${document.getElementById('perfSemester').value}`;
    executePostTransaction('/students', params, 'performanceModal', loadFacultyWorkspaceMetrics);
});

document.getElementById('disciplineForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const params = `action=LOG_DISCIPLINE&studentId=${encodeURIComponent(document.getElementById('discStudentId').value)}` +
                   `&points=${document.getElementById('discPoints').value}` +
                   `&desc=${encodeURIComponent(document.getElementById('discDesc').value)}`;
    executePostTransaction('/students', params, 'disciplineModal', loadFacultyWorkspaceMetrics);
});

document.getElementById('timetableForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const params = `action=ADD_TIMETABLE&type=${document.getElementById('ttType').value}` +
                   `&subjectCode=${encodeURIComponent(document.getElementById('ttSubjectSelect').value)}` +
                   `&day=${encodeURIComponent(document.getElementById('ttDay').value)}` +
                   `&slot=${encodeURIComponent(document.getElementById('ttSlot').value)}` +
                   `&room=${encodeURIComponent(document.getElementById('ttRoom').value)}`;
    executePostTransaction('/admin/users', params, 'timetableModal', loadAdminWorkspaceMetrics);
});

document.getElementById('feeForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const params = `action=ADD_INVOICE&studentId=${encodeURIComponent(document.getElementById('feeStudentId').value)}` +
                   `&amount=${document.getElementById('feeAmount').value}`;
    executePostTransaction('/admin/users', params, 'feeModal', loadAdminWorkspaceMetrics);
});

document.getElementById('assignmentForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const params = `action=ADD_ASSIGNMENT&title=${encodeURIComponent(document.getElementById('assignTitle').value)}` +
                   `&subjectCode=${encodeURIComponent(document.getElementById('assignSubjectSelect').value)}` +
                   `&due=${encodeURIComponent(document.getElementById('assignDue').value)}` +
                   `&attachment=${encodeURIComponent(document.getElementById('assignAttachment').value)}`;
    executePostTransaction('/students', params, 'assignmentModal', loadFacultyWorkspaceMetrics);
});

document.getElementById('submitAssignmentForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const params = `action=SUBMIT_ASSIGNMENT&studentId=${encodeURIComponent(currentSessionUser.id)}` +
                   `&assignmentId=${document.getElementById('subAssignId').value}`;
    executePostTransaction('/students', params, 'submitAssignmentModal', loadStudentWorkspaceMetrics);
});

document.getElementById('leaveForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const params = `action=FILE_LEAVE&studentId=${encodeURIComponent(currentSessionUser.id)}` +
                   `&start=${encodeURIComponent(document.getElementById('leaveStart').value)}` +
                   `&end=${encodeURIComponent(document.getElementById('leaveEnd').value)}` +
                   `&reason=${encodeURIComponent(document.getElementById('leaveReason').value)}`;
    executePostTransaction('/students', params, 'leaveModal', loadStudentWorkspaceMetrics);
});

function executePostTransaction(endpoint, bodyDataString, modalToClose, uiRefreshCallback) {
    fetch(endpoint, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: bodyDataString })
    .then(res => {
        if (!res.ok) throw new Error("Operational system transaction failure.");
        alert("Transaction committed successfully.");
        closeModal(modalToClose);
        if(uiRefreshCallback) uiRefreshCallback();
        const f = document.querySelector(`#${modalToClose} form`); if(f) f.reset();
    })
    .catch(err => alert(err.message));
}

function loadAdminWorkspaceMetrics() {
    fetch('/admin/users?fetchMode=ALL_USERS').then(res => res.json()).then(users => {
        document.getElementById('adminStatUsers').innerText = users.length;
        const tbody = document.getElementById('adminUserTableBody'); tbody.innerHTML = '';
        users.forEach(u => { tbody.innerHTML += `<tr><td><b>${u.id}</b></td><td><span class="badge on-track">${u.role}</span></td><td>${u.createdAt}</td></tr>`; });
    });

    fetch('/admin/users?fetchMode=ALL_SUBJECTS').then(res => res.json()).then(subjects => {
        const tbody = document.getElementById('adminSubjectTableBody'); tbody.innerHTML = '';
        subjects.forEach(sub => {
            let facIdDisplay = sub.facultyId ? `<b>${sub.facultyId}</b>` : '<span class="badge at-risk">Unassigned</span>';
            tbody.innerHTML += `<tr><td><code>${sub.code}</code></td><td>${sub.name}</td><td>${facIdDisplay}</td></tr>`;
        });
    });
}

function loadFacultyWorkspaceMetrics() {
    fetch(`/students?fetchMode=GET_PROFILE_CARD_METADATA&role=FACULTY&id=${encodeURIComponent(currentSessionUser.id)}`)
    .then(res => res.json())
    .then(p => {
        document.getElementById('facultyProfileName').innerText = p.name;
        document.getElementById('facultyProfileId').innerText = p.id;
    });

    fetch('/students?fetchMode=BULK_COHORT').then(res => res.json()).then(data => {
        document.getElementById('facStatTotal').innerText = data.length;
        const tbody = document.getElementById('facultyStudentTableBody'); tbody.innerHTML = '';
        let defaulterCount = 0; let gradeMetricsBuckets = { 'A': 0, 'B': 0, 'C': 0, 'F': 0 };

        data.forEach(s => {
            if (s.attendanceRate < 75) defaulterCount++;
            let g = s.gradeLetter || 'F'; if(gradeMetricsBuckets[g] !== undefined) gradeMetricsBuckets[g]++;
            tbody.innerHTML += `<tr><td><b>#${s.id}</b></td><td>${s.name}</td><td>${s.email}</td><td><b>${s.attendanceRate}%</b></td><td><span style="color:var(--accent-blue);font-weight:700;">${g}</span></td><td><span class="badge on-track">${s.behaviorScore} Pts</span></td></tr>`;
        });
        document.getElementById('facStatRisk').innerText = defaulterCount;
        renderAnalyticsChart(gradeMetricsBuckets);
    });

    fetch('/students?fetchMode=FACULTY_ENROLLMENT_LOGS').then(res => res.json()).then(logs => {
        const tbody = document.getElementById('facultyEnrollmentTableBody'); tbody.innerHTML = '';
        logs.forEach(l => { tbody.innerHTML += `<tr><td><code>${l.studentId}</code></td><td><b>${l.subjectCode}</b></td><td>${l.internal} Marks</td></tr>`; });
    });

    fetch('/students?fetchMode=FACULTY_LEAVE_VIEW_ALL').then(res => res.json()).then(leaves => {
        const tbody = document.getElementById('facultyLeaveTableBody'); tbody.innerHTML = '';
        leaves.forEach(lv => {
            let badgeClass = lv.status === 'APPROVED' ? 'excellent' : lv.status === 'PENDING' ? 'on-track' : 'at-risk';
            let actionsMarkup = lv.status === 'PENDING' ? 
                `<button onclick="updateLeaveRequestStatus(${lv.id}, 'APPROVED')" style="background:none; border:none; color:var(--success); font-weight:700; margin-right:10px; cursor:pointer;">Approve</button>` +
                `<button onclick="updateLeaveRequestStatus(${lv.id}, 'REJECTED')" style="background:none; border:none; color:var(--danger); font-weight:700; cursor:pointer;">Reject</button>` : 
                `<span style="color:var(--text-muted); font-size:0.85rem;">Concluded</span>`;
            tbody.innerHTML += `<tr><td>#${lv.id}</td><td><b>${lv.studentId}</b></td><td>${lv.start}</td><td>${lv.end}</td><td>text: "${lv.reason}"</td><td><span class="badge ${badgeClass}">${lv.status}</span></td><td>${actionsMarkup}</td></tr>`;
        });
    });
}

function loadStudentWorkspaceMetrics() {
    fetch(`/students?fetchMode=GET_PROFILE_CARD_METADATA&role=STUDENT&id=${encodeURIComponent(currentSessionUser.id)}`)
    .then(res => res.json())
    .then(p => {
        document.getElementById('studentProfileName').innerText = p.name;
        document.getElementById('studentProfileId').innerText = p.id;
    });

    fetch(`/students?fetchMode=SINGLE_PROFILE&id=${encodeURIComponent(currentSessionUser.id)}`).then(res => res.json()).then(profile => {
        document.getElementById('stuAttendance').innerText = profile.attendanceRate + '%';
        document.getElementById('stuGPA').innerText = profile.gpa.toFixed(2);
        document.getElementById('stuBehavior').innerText = profile.behaviorScore;
        document.getElementById('stuWarningAlert').style.display = (profile.attendanceRate < 75 || profile.behaviorScore < 85) ? 'block' : 'none';

        const ttBody = document.getElementById('studentTimetableBody'); ttBody.innerHTML = '';
        profile.timetableRoster.forEach(slot => {
            ttBody.innerHTML += `<tr><td><b>${slot.day}</b></td><td><code>${slot.subject}</code></td><td>${slot.time}</td><td><span class="badge on-track">${slot.room}</span></td></tr>`;
        });
        document.getElementById('studentFeeInvoice').innerText = `₹${profile.pendingInvoiceFee.toLocaleString('en-IN', {minimumFractionDigits: 2})} Outstanding Dues`;

        const leaveBody = document.getElementById('studentLeaveTableBody'); leaveBody.innerHTML = '';
        profile.leaveRoster.forEach(lv => {
            let badgeClass = lv.status === 'APPROVED' ? 'excellent' : lv.status === 'PENDING' ? 'on-track' : 'at-risk';
            leaveBody.innerHTML += `<tr><td>${lv.start}</td><td>${lv.end}</td><td>${lv.reason}</td><td><span class="badge ${badgeClass}">${lv.status}</span></td></tr>`;
        });

        const granularBody = document.getElementById('studentSubjectGranularTableBody'); granularBody.innerHTML = '';
        profile.granularSubjectBreakdown.forEach(sub => {
            granularBody.innerHTML += `<tr><td><code>${sub.subjectCode}</code></td><td><b>${sub.subjectName}</b></td><td><b>${sub.subjectAttendance}%</b></td><td>${sub.internalAssessment} / 20</td><td>${sub.assignmentScore} / 30</td><td>${sub.semesterExam} / 50</td><td><span style="color:var(--accent-blue); font-weight:700;">${sub.subjectGPA.toFixed(2)}</span></td></tr>`;
        });
    });
}

function loadParentWorkspaceMetrics() {
    fetch(`/students?fetchMode=GET_PROFILE_CARD_METADATA&role=PARENT&id=${encodeURIComponent(currentSessionUser.id)}`)
    .then(res => res.json())
    .then(p => {
        document.getElementById('parentProfileName').innerText = p.name;
        document.getElementById('parentProfileId').innerText = p.id;
    });

    fetch(`/students?fetchMode=PARENT_LOOKUP&parentId=${encodeURIComponent(currentSessionUser.id)}`).then(res => res.json()).then(ward => {
        document.getElementById('pWardName').innerText = ward.name;
        document.getElementById('pWardAttendance').innerText = ward.attendanceRate + '%';
        document.getElementById('pWardGrade').innerText = ward.gradeLetter;
    });
}

function renderAnalyticsChart(buckets) {
    const ctx = document.getElementById('gradeDistributionChart').getContext('2d');
    if (distributionChartInstance) distributionChartInstance.destroy();
    distributionChartInstance = new Chart(ctx, {
        type: 'bar',
        data: { labels: Object.keys(buckets), datasets: [{ data: Object.values(buckets), backgroundColor: ['#38bdf8', '#10b981', '#f59e0b', '#ef4444'] }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }
    });
}

function handleLogout() { currentSessionUser = null; document.getElementById('portalSection').style.display = 'none'; document.getElementById('loginSection').style.display = 'flex'; document.getElementById('loginForm').reset(); }