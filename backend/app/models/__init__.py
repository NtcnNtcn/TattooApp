# Models package – import all so Alembic can discover them
from app.models.user import User, Role, RoleName  # noqa: F401
from app.models.tattoo_work import TattooWork, Tag, work_tags, WorkStatus  # noqa: F401
from app.models.social import Favorite, Subscription, Like  # noqa: F401
from app.models.review import WorkReview, Notification  # noqa: F401
from app.models.application import ConsultationApplication, ApplicationStatus  # noqa: F401
from app.models.verification import VerificationCode, VerificationType  # noqa: F401
